package io.github.headlesshq.headlessmc.web.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.smallrye.common.os.ProcessInfo;

import java.util.List;

// A Web Image has no ProcessHandle, so the static initializer of io.smallrye.common.os.Process fails.
// Process is initialized at build time (see native-image.properties) with its fields reset,
// and the methods using them are replaced with fixed values.

@TargetClass(className = "io.smallrye.common.os.Process")
final class Target_io_smallrye_common_os_Process {
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private static ProcessHandle current;

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private static ProcessHandle.Info currentInfo;

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private static String name;

    @Substitute
    public static String getProcessName() {
        return "headlessmc";
    }

    @Substitute
    public static long getProcessId() {
        return 0L;
    }

    @Substitute
    public static ProcessInfo getCurrentProcess() {
        return new ProcessInfo(0L, "headlessmc");
    }

    @Substitute
    public static List<ProcessInfo> getAllProcesses() {
        return List.of(getCurrentProcess());
    }

}
