// Shows version, stars and forks of the GitHub repository next to its name, like the source widget of mkdocs-material.
// The facts are cached in the sessionStorage to stay below the rate limit of the GitHub API.
(() => {
    "use strict";

    const REPOSITORY = "headlesshq/headlessmc";
    const CACHE_KEY = "headlessmc.repository";
    const facts = document.getElementById("source-facts");

    // Octicons by GitHub, MIT License
    const ICONS = {
        version: "M1 7.775V2.75C1 1.784 1.784 1 2.75 1h5.025c.464 0 .91.184 1.238.513l6.25 6.25a1.75 1.75 0 0 1 0 2.474l-5.026 5.026a1.75 1.75 0 0 1-2.474 0l-6.25-6.25A1.75 1.75 0 0 1 1 7.775Zm1.5 0c0 .066.026.13.073.177l6.25 6.25a.25.25 0 0 0 .354 0l5.025-5.025a.25.25 0 0 0 0-.354l-6.25-6.25a.25.25 0 0 0-.177-.073H2.75a.25.25 0 0 0-.25.25ZM6 5a1 1 0 1 1 0 2 1 1 0 0 1 0-2Z",
        stars: "M8 .25a.75.75 0 0 1 .673.418l1.882 3.815 4.21.612a.75.75 0 0 1 .416 1.279l-3.046 2.97.719 4.192a.751.751 0 0 1-1.088.791L8 12.347l-3.766 1.98a.75.75 0 0 1-1.088-.79l.72-4.194L.818 6.374a.75.75 0 0 1 .416-1.28l4.21-.611L7.327.668A.75.75 0 0 1 8 .25Zm0 2.445L6.615 5.5a.75.75 0 0 1-.564.41l-3.097.45 2.24 2.184a.75.75 0 0 1 .216.664l-.528 3.084 2.769-1.456a.75.75 0 0 1 .698 0l2.77 1.456-.53-3.084a.75.75 0 0 1 .216-.664l2.24-2.183-3.096-.45a.75.75 0 0 1-.564-.41L8 2.694Z",
        forks: "M5 5.372v.878c0 .414.336.75.75.75h4.5a.75.75 0 0 0 .75-.75v-.878a2.25 2.25 0 1 1 1.5 0v.878a2.25 2.25 0 0 1-2.25 2.25h-1.5v2.128a2.251 2.251 0 1 1-1.5 0V8.5h-1.5A2.25 2.25 0 0 1 3.5 6.25v-.878a2.25 2.25 0 1 1 1.5 0ZM5 3.25a.75.75 0 1 0-1.5 0 .75.75 0 0 0 1.5 0Zm6.75.75a.75.75 0 1 0 0-1.5.75.75 0 0 0 0 1.5Zm-3 8.75a.75.75 0 1 0-1.5 0 .75.75 0 0 0 1.5 0Z",
    };

    // same rounding as mkdocs-material: 1234 -> 1.2k
    function round(value) {
        if (value > 999) {
            const digits = +((value - 950) % 1000 > 99);
            return `${((value + 0.000001) / 1000).toFixed(digits)}k`;
        }

        return value.toString();
    }

    function render(data) {
        const entries = [];
        if (data.version) {
            entries.push(["version", data.version, "Latest release"]);
        }

        entries.push(["stars", round(data.stars), "Stars"], ["forks", round(data.forks), "Forks"]);
        for (const [kind, text, title] of entries) {
            const li = document.createElement("li");
            li.className = `source-fact source-fact--${kind}`;
            li.title = title;
            const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
            svg.setAttribute("viewBox", "0 0 16 16");
            svg.setAttribute("aria-hidden", "true");
            const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
            path.setAttribute("d", ICONS[kind]);
            svg.appendChild(path);
            li.append(svg, text);
            facts.appendChild(li);
        }

        requestAnimationFrame(() => facts.classList.add("loaded"));
    }

    async function load() {
        const api = `https://api.github.com/repos/${REPOSITORY}`;
        const [repository, release] = await Promise.all([
            fetch(api).then((response) => response.ok ? response.json() : Promise.reject(response.status)),
            fetch(`${api}/releases/latest`).then((response) => response.ok ? response.json() : null).catch(() => null),
        ]);

        return {
            version: release && release.tag_name ? release.tag_name : null,
            stars: repository.stargazers_count,
            forks: repository.forks_count,
        };
    }

    try {
        const cached = JSON.parse(sessionStorage.getItem(CACHE_KEY));
        if (cached) {
            render(cached);
            return;
        }
    } catch (e) {
        // no cache
    }

    load().then((data) => {
        render(data);
        try {
            sessionStorage.setItem(CACHE_KEY, JSON.stringify(data));
        } catch (e) {
            // no cache
        }
    }).catch(() => {
        // offline or rate limited, just show the name of the repository
    });
})();
