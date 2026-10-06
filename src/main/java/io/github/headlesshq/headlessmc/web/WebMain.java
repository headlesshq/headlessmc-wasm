package io.github.headlesshq.headlessmc.web;

import io.github.headlesshq.headlessmc.config.ConfigFileProvider;
import io.github.headlesshq.headlessmc.console.ArgSplitter;
import io.github.headlesshq.headlessmc.files.AppFiles;
import io.github.headlesshq.headlessmc.files.FileService;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.runtime.Application;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.graalvm.webimage.api.JSNumber;
import org.graalvm.webimage.api.JSString;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * The entrypoint of HeadlessMc running as a WebAssembly module in the browser.
 * <p>
 * A Web Image has a single thread, and the browser can only deliver input
 * to it while it is not running, so there is no blocking read loop on {@link System#in}.
 * Instead, this starts Quarkus without the {@link io.quarkus.runtime.ApplicationLifecycleManager}
 * (which would stop the application as soon as main returns, and which needs signals and shutdown hooks),
 * hands the page a function that executes one line, and returns.
 * The page then calls that function for every line typed into the terminal.
 */
@QuarkusMain
@RegisterForReflection(classNames = WebMain.APPLICATION_IMPL)
public class WebMain {
    static final String APPLICATION_IMPL = "io.quarkus.runner.ApplicationImpl";

    @SuppressWarnings("UnnecessaryModifier")
    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(new WebOutputStream("out"), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new WebOutputStream("err"), true, StandardCharsets.UTF_8));

        Application application = (Application) Class.forName(APPLICATION_IMPL).getDeclaredConstructor().newInstance();
        application.start(args);

        ArcContainer container = Arc.container();
        initFiles(container);
        ArgSplitter argSplitter = container.instance(ArgSplitter.class).get();
        System.out.println("HeadlessMc running in WebAssembly. Type 'help' for a list of commands.");
        WebCompletions completions = new WebCompletions(container, argSplitter);
        WebBridge.ready(
            line -> JSNumber.of(execute(container, argSplitter, line.asString())),
            line -> JSString.of(complete(completions, line.asString()))
        );
    }

    private static int execute(ArcContainer container, ArgSplitter argSplitter, String line) {
        String[] args = argSplitter.split(line);
        if (args.length == 0) {
            // without arguments HeadlessMcCommand would start its own blocking read loop
            return 0;
        }

        try {
            CommandLine commandLine = container.instance(CommandLine.class).get();
            commandLine.setUsageHelpWidth(100);
            commandLine.setExecutionExceptionHandler((exception, cl, parseResult) -> {
                printError(exception);
                return cl.getCommandSpec().exitCodeOnExecutionException();
            });

            return commandLine.execute(args);
        } catch (Throwable t) {
            printError(t);
            return 1;
        } finally {
            System.out.flush();
            System.err.flush();
        }
    }

    /**
     * Prints the messages of the throwable and its causes.
     * The stacktraces of a Web Image only consist of Wasm function indices and are of no use.
     */
    private static void printError(Throwable throwable) {
        for (Throwable t = throwable; t != null; t = t.getCause() == t ? null : t.getCause()) {
            String message = t.getMessage();
            if (t.getClass().getName().equals("com.oracle.svm.core.jdk.UnsupportedFeatureError")
                    && message != null && message.contains("java.net.Socket")) {
                message = "Network access is not available in the browser.";
            }

            System.err.println((t == throwable ? "" : "Caused by: ") + t.getClass().getSimpleName()
                + (message == null ? "" : ": " + message));
        }
    }

    private static String complete(WebCompletions completions, String line) {
        try {
            return completions.complete(line);
        } catch (Throwable t) {
            // completions are best effort, do not spam the terminal
            return "";
        }
    }

    private static void initFiles(ArcContainer container) {
        AppFiles appFiles = container.instance(AppFiles.class).get();
        try {
            Files.createDirectories(appFiles.getDataDir());
            Files.createDirectories(appFiles.getConfigDir());
            container.instance(FileService.class).get().ensureFileExists(
                container.instance(ConfigFileProvider.class).get().getConfigFile(),
                ("# === HeadlessMc Config ===" + System.lineSeparator()).getBytes(StandardCharsets.UTF_8)
            );
        } catch (IOException e) {
            printError(e);
        }
    }

}
