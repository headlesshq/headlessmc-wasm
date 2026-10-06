package io.github.headlesshq.headlessmc.web;

import io.github.headlesshq.headlessmc.distribution.JavaDistribution;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionException;
import io.github.headlesshq.headlessmc.distribution.JavaDistributionProvider;
import io.github.headlesshq.headlessmc.distribution.JavaRuntime;
import io.github.headlesshq.headlessmc.os.CPU;
import io.github.headlesshq.headlessmc.os.OS;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Path;
import java.util.List;

/**
 * Replaces the foojay.io provider, which needs the Quarkus REST client,
 * which cannot be compiled to WebAssembly.
 */
@ApplicationScoped
public class UnavailableJavaDistributionProvider implements JavaDistributionProvider {
    @Override
    public String getName() {
        return JavaDistributionProvider.DEFAULT;
    }

    @Override
    public List<JavaDistribution> getDistributions() {
        throw unavailable();
    }

    @Override
    public JavaDistribution getDistributionByName(String name) {
        throw unavailable();
    }

    @Override
    public List<JavaRuntime> getRuntimes(JavaDistribution distribution, int version, OS os, CPU cpu) {
        throw unavailable();
    }

    @Override
    public List<JavaRuntime> getUpdates(JavaRuntime runtime) {
        throw unavailable();
    }

    @Override
    public void download(JavaRuntime runtime, Path path) {
        throw unavailable();
    }

    private static JavaDistributionException unavailable() {
        return new JavaDistributionException("Downloading Java is not available in the browser.");
    }

}
