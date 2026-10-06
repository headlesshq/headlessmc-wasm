package io.github.headlesshq.headlessmc.web;

import org.graalvm.webimage.api.JS;
import org.graalvm.webimage.api.JSNumber;
import org.graalvm.webimage.api.JSString;

import java.util.function.Function;

/**
 * The boundary between HeadlessMc and the JavaScript page hosting it.
 * The page must define {@code globalThis.hmc} with a {@code write(stream, text)}
 * and a {@code ready(execute, complete)} function before the Wasm module is loaded, see {@code webapp/terminal.js}.
 * These methods can only be called when running as a Web Image.
 */
final class WebBridge {
    private WebBridge() {
        throw new AssertionError();
    }

    /**
     * Writes text to the terminal of the page.
     *
     * @param stream "out" or "err".
     * @param text the text to write, may contain ANSI escape codes and newlines.
     */
    @JS.Coerce
    @JS(args = {"stream", "text"}, value = "globalThis.hmc.write(stream, text);")
    static native void write(String stream, String text);

    /**
     * Hands the page the functions it needs to drive HeadlessMc.
     *
     * @param execute executes a line typed into the terminal, returns the exit code of the command.
     * @param complete returns the completion candidates for the last word of a line, separated by newlines.
     */
    @JS(args = {"execute", "complete"}, value = "globalThis.hmc.ready(execute, complete);")
    static native void ready(Function<JSString, JSNumber> execute, Function<JSString, JSString> complete);

}
