// The page side of HeadlessMc running as a GraalVM Web Image.
// Must be loaded before headlessmc.js: it defines globalThis.hmc, which the Java side calls
// (see io.github.headlesshq.headlessmc.web.WebBridge), and wraps fetch to show download progress.
(() => {
    "use strict";

    const terminal = document.getElementById("terminal");
    const output = document.getElementById("output");
    const form = document.getElementById("prompt");
    const input = document.getElementById("input");
    const status = document.getElementById("status");
    const loader = document.getElementById("loader");
    const loaderText = document.getElementById("loader-text");
    const loaderBar = document.getElementById("loader-bar");

    const HISTORY_KEY = "headlessmc.history";
    const HISTORY_SIZE = 200;

    let execute = null;
    let complete = null;
    let busy = false;
    let history = loadHistory();
    let historyIndex = history.length;
    let draft = "";

    function setStatus(state, text) {
        status.dataset.state = state;
        status.textContent = text;
    }

    function scrollToBottom() {
        terminal.scrollTop = terminal.scrollHeight;
    }

    // ---- output with a minimal ANSI SGR parser ----

    const ANSI_COLORS = ["#4d5566", "#ff7b72", "#7ee787", "#e3b341", "#79c0ff", "#d2a8ff", "#56d4dd", "#d7dae0"];
    const ANSI_BRIGHT = ["#7d8590", "#ffa198", "#aff5b4", "#f8e3a1", "#a5d6ff", "#e2c5ff", "#b3f0ff", "#ffffff"];
    const sgr = {fg: null, bold: false};

    function applySgr(codes) {
        for (let i = 0; i < codes.length; i++) {
            const code = codes[i];
            if (code === 0) {
                sgr.fg = null;
                sgr.bold = false;
            } else if (code === 1) {
                sgr.bold = true;
            } else if (code === 22) {
                sgr.bold = false;
            } else if (code >= 30 && code <= 37) {
                sgr.fg = ANSI_COLORS[code - 30];
            } else if (code >= 90 && code <= 97) {
                sgr.fg = ANSI_BRIGHT[code - 90];
            } else if (code === 39) {
                sgr.fg = null;
            } else if (code === 38 && codes[i + 1] === 5) {
                i += 2; // 256 colors, not supported, skip the argument
            } else if (code === 38 && codes[i + 1] === 2) {
                sgr.fg = `rgb(${codes[i + 2]}, ${codes[i + 3]}, ${codes[i + 4]})`;
                i += 4;
            }
        }
    }

    function appendText(text, className) {
        const fragment = document.createDocumentFragment();
        const pattern = /\x1b\[([0-9;]*)([A-Za-z])/g;
        let last = 0;
        let match;
        const flush = (part) => {
            if (!part) {
                return;
            }

            const span = document.createElement("span");
            span.textContent = part;
            if (className) {
                span.className = className;
            }

            if (sgr.fg) {
                span.style.color = sgr.fg;
            }

            if (sgr.bold) {
                span.style.fontWeight = "bold";
            }

            fragment.appendChild(span);
        };

        while ((match = pattern.exec(text)) !== null) {
            flush(text.slice(last, match.index));
            if (match[2] === "m") {
                applySgr(match[1] === "" ? [0] : match[1].split(";").map(Number));
            }

            last = pattern.lastIndex;
        }

        flush(text.slice(last));
        output.appendChild(fragment);
        scrollToBottom();
    }

    function echo(line) {
        const div = document.createElement("div");
        div.className = "echo";
        const sign = document.createElement("span");
        sign.className = "prompt-sign";
        sign.textContent = "> ";
        div.append(sign, line);
        output.appendChild(div);
        scrollToBottom();
    }

    // ---- bridge called from Java ----

    globalThis.hmc = {
        write(stream, text) {
            appendText(text, stream === "err" ? "err" : null);
        },

        ready(executeFunction, completeFunction) {
            execute = executeFunction;
            complete = completeFunction;
            loader.hidden = true;
            form.hidden = false;
            setStatus("ready", "Ready");
            input.focus();
            scrollToBottom();
        },
    };

    // ---- running commands ----

    function run(line) {
        echo(line);
        remember(line);
        const trimmed = line.trim();
        if (trimmed === "clear" || trimmed === "cls") {
            output.replaceChildren();
            return;
        }

        if (trimmed === "exit" || trimmed === "quit") {
            appendText("There is nothing to exit, just close the tab.\n");
            return;
        }

        busy = true;
        input.disabled = true;
        setStatus("running", "Running");
        // HeadlessMc runs on the main thread, give the browser a chance to paint the echo first
        setTimeout(() => {
            try {
                execute(line);
            } catch (e) {
                // Java exceptions are handled on the Java side, this is a crash of the Wasm module itself.
                console.error(e);
                appendText(`Internal error: ${e}\n`, "err");
            } finally {
                busy = false;
                input.disabled = false;
                setStatus("ready", "Ready");
                input.focus();
                scrollToBottom();
            }
        }, 16);
    }

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        if (!execute || busy) {
            return;
        }

        const line = input.value;
        input.value = "";
        run(line);
    });

    // ---- history ----

    function loadHistory() {
        try {
            const stored = JSON.parse(localStorage.getItem(HISTORY_KEY) || "[]");
            return Array.isArray(stored) ? stored.filter((entry) => typeof entry === "string") : [];
        } catch (e) {
            return [];
        }
    }

    function remember(line) {
        if (line.trim() !== "" && history[history.length - 1] !== line) {
            history.push(line);
            history = history.slice(-HISTORY_SIZE);
            try {
                localStorage.setItem(HISTORY_KEY, JSON.stringify(history));
            } catch (e) {
                // history is just a convenience
            }
        }

        historyIndex = history.length;
        draft = "";
    }

    function showHistory(index) {
        if (historyIndex === history.length) {
            draft = input.value;
        }

        historyIndex = Math.max(0, Math.min(history.length, index));
        input.value = historyIndex === history.length ? draft : history[historyIndex];
        input.setSelectionRange(input.value.length, input.value.length);
    }

    // ---- completion ----

    function commonPrefix(words) {
        let prefix = words[0];
        for (const word of words) {
            while (!word.startsWith(prefix)) {
                prefix = prefix.slice(0, -1);
            }
        }

        return prefix;
    }

    function completeInput() {
        if (!complete || busy) {
            return;
        }

        const line = input.value;
        let result;
        try {
            result = complete(line);
        } catch (e) {
            console.error(e);
            return;
        }

        const candidates = String(result).split("\n").filter((candidate) => candidate !== "");
        if (candidates.length === 0) {
            return;
        }

        const start = line.search(/\S*$/);
        const word = line.slice(start);
        if (candidates.length === 1) {
            input.value = line.slice(0, start) + candidates[0] + " ";
            return;
        }

        const prefix = commonPrefix(candidates);
        if (prefix.length > word.length) {
            input.value = line.slice(0, start) + prefix;
        } else {
            echo(line);
            appendText(candidates.join("   ") + "\n");
        }
    }

    input.addEventListener("keydown", (event) => {
        if (event.key === "ArrowUp") {
            event.preventDefault();
            showHistory(historyIndex - 1);
        } else if (event.key === "ArrowDown") {
            event.preventDefault();
            showHistory(historyIndex + 1);
        } else if (event.key === "Tab") {
            event.preventDefault();
            completeInput();
        } else if (event.key === "l" && event.ctrlKey) {
            event.preventDefault();
            output.replaceChildren();
        } else if (event.key === "c" && event.ctrlKey && input.selectionStart === input.selectionEnd) {
            event.preventDefault();
            echo(input.value + "^C");
            input.value = "";
            historyIndex = history.length;
        }
    });

    // clicking into the terminal focuses the input, unless text is being selected
    terminal.addEventListener("mouseup", () => {
        if (!form.hidden && String(window.getSelection()) === "") {
            input.focus({preventScroll: true});
        }
    });

    // ---- loading ----

    function formatMegabytes(bytes) {
        return (bytes / (1024 * 1024)).toFixed(1);
    }

    // The Web Image loader fetches headlessmc.js.wasm with fetch and reads it with arrayBuffer().
    // We hand it a response that counts the bytes as they arrive.
    const originalFetch = globalThis.fetch.bind(globalThis);
    globalThis.fetch = async (resource, options) => {
        const response = await originalFetch(resource, options);
        const url = typeof resource === "string" ? resource : resource.url;
        if (!url.endsWith(".wasm") || !response.ok || !response.body) {
            return response;
        }

        // Content-Length is the compressed size if the server compresses, then it is only an estimate.
        const total = Number(response.headers.get("Content-Length")) || 0;
        const encoded = response.headers.get("Content-Encoding");
        const knownTotal = total > 0 && !encoded;
        if (!knownTotal) {
            loaderBar.classList.add("indeterminate");
        }

        let loaded = 0;
        const reader = response.body.getReader();
        const stream = new ReadableStream({
            async pull(controller) {
                const {done, value} = await reader.read();
                if (done) {
                    loaderText.textContent = "Starting HeadlessMc…";
                    loaderBar.classList.remove("indeterminate");
                    loaderBar.style.width = "100%";
                    setStatus("loading", "Starting");
                    controller.close();
                    return;
                }

                loaded += value.byteLength;
                if (knownTotal) {
                    loaderBar.style.width = `${Math.min(100, (loaded / total) * 100)}%`;
                    loaderText.textContent = `Downloading HeadlessMc… ${formatMegabytes(loaded)} / ${formatMegabytes(total)} MB`;
                } else {
                    loaderText.textContent = `Downloading HeadlessMc… ${formatMegabytes(loaded)} MB`;
                }

                controller.enqueue(value);
            },
            cancel(reason) {
                reader.cancel(reason);
            },
        });

        return new Response(stream, {headers: response.headers, status: response.status, statusText: response.statusText});
    };

    function fail(reason) {
        if (execute) {
            return;
        }

        console.error(reason);
        loader.hidden = true;
        setStatus("error", "Failed");
        const message = String(reason && reason.message ? reason.message : reason);
        const unsupported = /CompileError|wasm|WebAssembly/i.test(message) && !/fetch|Failed to load/i.test(message);
        appendText(`Failed to start HeadlessMc: ${message}\n`, "err");
        if (unsupported) {
            appendText("HeadlessMc needs WebAssembly GC and exception handling, "
                + "use a current version of Chrome, Edge, Firefox or Safari.\n", "err");
        }

        if (location.protocol === "file:") {
            appendText("The page has to be served over http, e.g. python3 -m http.server\n", "err");
        }
    }

    window.addEventListener("unhandledrejection", (event) => fail(event.reason));
    window.addEventListener("error", (event) => fail(event.error || event.message));
})();
