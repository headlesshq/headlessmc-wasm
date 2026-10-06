package io.github.headlesshq.headlessmc.web.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import jdk.internal.misc.Unsafe;

import java.lang.invoke.VarHandle;
import java.util.Objects;

// The Web Image backend (GraalVM 25.4) fails to compile compareAndSet/compareAndExchange on double and float
// field VarHandles ("unhandled compare ... EQ" in WasmGCSingleThreadedAtomicsPhase), which are always reachable
// as soon as any VarHandle is used. A Web Image is single-threaded, so we just compare and swap the raw bits.

@TargetClass(className = "java.lang.invoke.VarHandleDoubles", innerClass = "FieldInstanceReadOnly")
final class Target_VarHandleDoubles_FieldInstanceReadOnly {
    @Alias
    long fieldOffset;
}

@TargetClass(className = "java.lang.invoke.VarHandleDoubles", innerClass = "FieldInstanceReadWrite")
final class Target_VarHandleDoubles_FieldInstanceReadWrite {
    @Substitute
    static boolean compareAndSet(VarHandle ob, Object holder, double expected, double value) {
        long offset = ((Target_VarHandleDoubles_FieldInstanceReadOnly) (Object) ob).fieldOffset;
        return VarHandleSubstitutions.casLong(Objects.requireNonNull(holder), offset, expected, value);
    }

    @Substitute
    static double compareAndExchange(VarHandle ob, Object holder, double expected, double value) {
        long offset = ((Target_VarHandleDoubles_FieldInstanceReadOnly) (Object) ob).fieldOffset;
        return VarHandleSubstitutions.caxLong(Objects.requireNonNull(holder), offset, expected, value);
    }
}

@TargetClass(className = "java.lang.invoke.VarHandleDoubles", innerClass = "FieldStaticReadOnly")
final class Target_VarHandleDoubles_FieldStaticReadOnly {
    @Alias
    Object base;
    @Alias
    long fieldOffset;
}

@TargetClass(className = "java.lang.invoke.VarHandleDoubles", innerClass = "FieldStaticReadWrite")
final class Target_VarHandleDoubles_FieldStaticReadWrite {
    @Substitute
    static boolean compareAndSet(VarHandle ob, double expected, double value) {
        Target_VarHandleDoubles_FieldStaticReadOnly handle = (Target_VarHandleDoubles_FieldStaticReadOnly) (Object) ob;
        return VarHandleSubstitutions.casLong(handle.base, handle.fieldOffset, expected, value);
    }

    @Substitute
    static double compareAndExchange(VarHandle ob, double expected, double value) {
        Target_VarHandleDoubles_FieldStaticReadOnly handle = (Target_VarHandleDoubles_FieldStaticReadOnly) (Object) ob;
        return VarHandleSubstitutions.caxLong(handle.base, handle.fieldOffset, expected, value);
    }
}

@TargetClass(className = "java.lang.invoke.VarHandleFloats", innerClass = "FieldInstanceReadOnly")
final class Target_VarHandleFloats_FieldInstanceReadOnly {
    @Alias
    long fieldOffset;
}

@TargetClass(className = "java.lang.invoke.VarHandleFloats", innerClass = "FieldInstanceReadWrite")
final class Target_VarHandleFloats_FieldInstanceReadWrite {
    @Substitute
    static boolean compareAndSet(VarHandle ob, Object holder, float expected, float value) {
        long offset = ((Target_VarHandleFloats_FieldInstanceReadOnly) (Object) ob).fieldOffset;
        return VarHandleSubstitutions.casInt(Objects.requireNonNull(holder), offset, expected, value);
    }

    @Substitute
    static float compareAndExchange(VarHandle ob, Object holder, float expected, float value) {
        long offset = ((Target_VarHandleFloats_FieldInstanceReadOnly) (Object) ob).fieldOffset;
        return VarHandleSubstitutions.caxInt(Objects.requireNonNull(holder), offset, expected, value);
    }
}

@TargetClass(className = "java.lang.invoke.VarHandleFloats", innerClass = "FieldStaticReadOnly")
final class Target_VarHandleFloats_FieldStaticReadOnly {
    @Alias
    Object base;
    @Alias
    long fieldOffset;
}

@TargetClass(className = "java.lang.invoke.VarHandleFloats", innerClass = "FieldStaticReadWrite")
final class Target_VarHandleFloats_FieldStaticReadWrite {
    @Substitute
    static boolean compareAndSet(VarHandle ob, float expected, float value) {
        Target_VarHandleFloats_FieldStaticReadOnly handle = (Target_VarHandleFloats_FieldStaticReadOnly) (Object) ob;
        return VarHandleSubstitutions.casInt(handle.base, handle.fieldOffset, expected, value);
    }

    @Substitute
    static float compareAndExchange(VarHandle ob, float expected, float value) {
        Target_VarHandleFloats_FieldStaticReadOnly handle = (Target_VarHandleFloats_FieldStaticReadOnly) (Object) ob;
        return VarHandleSubstitutions.caxInt(handle.base, handle.fieldOffset, expected, value);
    }
}

final class VarHandleSubstitutions {
    private VarHandleSubstitutions() {
        throw new AssertionError();
    }

    static boolean casLong(Object base, long offset, double expected, double value) {
        return Unsafe.getUnsafe().compareAndSetLong(
            base, offset, Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(value));
    }

    static double caxLong(Object base, long offset, double expected, double value) {
        return Double.longBitsToDouble(Unsafe.getUnsafe().compareAndExchangeLong(
            base, offset, Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(value)));
    }

    static boolean casInt(Object base, long offset, float expected, float value) {
        return Unsafe.getUnsafe().compareAndSetInt(
            base, offset, Float.floatToRawIntBits(expected), Float.floatToRawIntBits(value));
    }

    static float caxInt(Object base, long offset, float expected, float value) {
        return Float.intBitsToFloat(Unsafe.getUnsafe().compareAndExchangeInt(
            base, offset, Float.floatToRawIntBits(expected), Float.floatToRawIntBits(value)));
    }

}
