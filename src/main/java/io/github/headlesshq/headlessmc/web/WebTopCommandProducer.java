package io.github.headlesshq.headlessmc.web;

import io.github.headlesshq.headlessmc.commands.HeadlessMcCommand;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

/**
 * Produces the root command for the {@link picocli.CommandLine} instance that quarkus-picocli will use.
 * Same as the TopCommandProducer of headlessmc-application.
 */
@Dependent
public class WebTopCommandProducer {
    /**
     * Produces the {@link TopCommand} for the quarkus-picocli command line.
     *
     * @return {@link HeadlessMcCommand}.class
     */
    @Produces
    @Dependent
    @TopCommand
    public Object topCommand() {
        return HeadlessMcCommand.class;
    }

}
