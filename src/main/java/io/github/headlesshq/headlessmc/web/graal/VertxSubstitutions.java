package io.github.headlesshq.headlessmc.web.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.vertx.core.Vertx;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

// quarkus-vertx comes in through quarkus-cache. It starts Vert.x eagerly during startup (e.g. to register
// event bus consumers), which starts Netty event loops, which need threads and epoll.
// HeadlessMc has no event bus consumers, so we just do not start Vert.x.

@TargetClass(className = "io.quarkus.vertx.runtime.VertxEventBusConsumerRecorder")
final class Target_io_quarkus_vertx_runtime_VertxEventBusConsumerRecorder {
    @Substitute
    public void configureVertx(Supplier<Vertx> vertx, List<?> messageConsumerConfigurations, LaunchMode launchMode,
                               ShutdownContext shutdown, Map<Class<?>, Class<?>> codecByClass, List<Class<?>> selectorTypes) {
        // do not start Vert.x
    }

    @Substitute
    public RuntimeValue<Vertx> forceStart(Supplier<Vertx> vertx) {
        return new RuntimeValue<>();
    }

}
