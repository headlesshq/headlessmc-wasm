package io.github.headlesshq.headlessmc.web.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// The default file system of a Web Image is an in-memory file system with the scheme "jimfs".
// SmallRye Config turns paths on the default file system into URIs (e.g. ${user.dir}/.env)
// and only accepts file, jar and http URIs, so we treat jimfs like file.

@TargetClass(className = "io.smallrye.config.AbstractLocationConfigSourceLoader")
final class Target_io_smallrye_config_AbstractLocationConfigSourceLoader {
    @Alias
    private static Converter<URI> URI_CONVERTER;

    @Alias
    protected native List<ConfigSource> tryFileSystem(URI uri, int ordinal);

    @Alias
    protected native List<ConfigSource> tryClassPath(URI uri, int ordinal, ClassLoader classLoader);

    @Alias
    protected native List<ConfigSource> tryJar(URI uri, int ordinal);

    @Alias
    protected native List<ConfigSource> tryHttpResource(URI uri, int ordinal);

    @Substitute
    protected List<ConfigSource> loadConfigSources(String[] locations, int ordinal, ClassLoader classLoader) {
        if (locations == null || locations.length == 0) {
            return Collections.emptyList();
        }

        List<ConfigSource> configSources = new ArrayList<>();
        for (String location : locations) {
            URI uri = URI_CONVERTER.convert(location);
            String scheme = uri.getScheme();
            if (scheme == null) {
                configSources.addAll(tryFileSystem(uri, ordinal));
                configSources.addAll(tryClassPath(uri, ordinal, classLoader));
            } else if (scheme.equals("file") || scheme.equals("jimfs")) {
                configSources.addAll(tryFileSystem(uri, ordinal));
            } else if (scheme.equals("jar")) {
                configSources.addAll(tryJar(uri, ordinal));
            } else if (scheme.startsWith("http")) {
                configSources.addAll(tryHttpResource(uri, ordinal));
            } else {
                throw new IllegalArgumentException("SRCFG00033: Scheme " + scheme + " not supported (web)");
            }
        }

        return configSources;
    }

}
