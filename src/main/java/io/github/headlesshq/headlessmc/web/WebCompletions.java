package io.github.headlesshq.headlessmc.web;

import io.github.headlesshq.headlessmc.console.ArgSplitter;
import io.github.headlesshq.headlessmc.console.Completions;
import io.github.headlesshq.headlessmc.console.completions.PicocliCompletions;
import io.quarkus.arc.ArcContainer;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Completes the word at the end of a line typed into the terminal of the page.
 */
@RequiredArgsConstructor
final class WebCompletions {
    private final ArcContainer container;
    private final ArgSplitter argSplitter;

    /**
     * @param line the line, the cursor is expected at its end.
     * @return the candidates for the last word, separated by newlines.
     */
    String complete(String line) {
        List<String> words = new ArrayList<>(Arrays.asList(argSplitter.split(line)));
        if (words.isEmpty() || line.isEmpty() || Character.isWhitespace(line.charAt(line.length() - 1))) {
            words.add("");
        }

        String word = words.getLast();
        Completions.Line parsed = new Completions.Line() {
            @Override
            public String word() {
                return word;
            }

            @Override
            public int wordCursor() {
                return word.length();
            }

            @Override
            public int wordIndex() {
                return words.size() - 1;
            }

            @Override
            public List<String> words() {
                return words;
            }

            @Override
            public String line() {
                return line;
            }

            @Override
            public int cursor() {
                return line.length();
            }
        };

        return new PicocliCompletions(() -> container.instance(CommandLine.class).get())
            .stream(parsed)
            .map(Completions.Candidate::getName)
            .distinct()
            .collect(Collectors.joining("\n"));
    }

}
