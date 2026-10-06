#!/bin/bash
# Builds HeadlessMc to WebAssembly with the GraalVM Web Image backend (native-image --tool:svm-wasm)
# and assembles a static website in build/web, serve it with e.g.
#   python3 -m http.server -d build/web 8000
#
# Requires:
#  - GRAALVM_HOME pointing to Oracle GraalVM 25.1 or later (Web Image is not part of GraalVM CE)
#  - binaryen (wasm-opt, version 119 or later) on the PATH

set -euo pipefail

if [ -z "${GRAALVM_HOME:-}" ]; then
    echo "GRAALVM_HOME is not set" >&2
    exit 1
fi

if ! command -v wasm-opt > /dev/null; then
    echo "wasm-opt (binaryen) is not on the PATH" >&2
    exit 1
fi

cd "$(dirname "$0")/.."

# Let Quarkus do the augmentation and generate the native-image arguments, but do not run native-image.
./gradlew quarkusBuild -Dquarkus.native.enabled=true -Dquarkus.package.jar.enabled=false \
    -Dquarkus.native.sources-only=true

SOURCES=build/native-sources
WORK=build/web-image
OUT=build/web
rm -rf "$WORK" "$OUT"
cp -r "$SOURCES" "$WORK"

# Quarkus substitutes java.util.logging.Logger.getLogger, which conflicts with the substitution of Web Image.
zip -q -d "$WORK"/lib/io.quarkus.quarkus-core-*.jar 'io/quarkus/runtime/graal/Target_java_util_logging_Logger*'
# The jline terminal backends calling into native code cannot be compiled to Wasm, jline is disabled anyway.
zip -q -d "$WORK"/lib/org.jline.jline-[0-9]*.jar 'org/jline/terminal/impl/ffm/*' 'org/jline/terminal/impl/jni/*' 'org/jline/nativ/*'
zip -q -d "$WORK"/lib/org.jline.jline-native-*.jar 'org/jline/nativ/*'

# --link-at-build-time: we have removed classes above, and a lot of code is not reachable in the browser anyway.
# --enable-monitoring and --enable-native-access are not supported by Web Image.
ARGS=$(sed \
    -e 's/--link-at-build-time//' \
    -e 's/--enable-monitoring=[^ ]*//' \
    -e 's/--enable-native-access=[^ ]*//' \
    "$WORK/native-image.args")

# Additional arguments can be passed with WEB_IMAGE_ARGS, e.g. "-H:+UnlockExperimentalVMOptions -H:+DebugNames"
(cd "$WORK" && "$GRAALVM_HOME/bin/native-image" --tool:svm-wasm $ARGS ${WEB_IMAGE_ARGS:-})

mkdir -p "$OUT"
cp src/main/webapp/* headlessmc/logo.svg "$OUT/"
cp "$WORK"/headlessmc-web-*-runner.js "$OUT/headlessmc.js"
cp "$WORK"/headlessmc-web-*-runner.js.wasm "$OUT/headlessmc.js.wasm"
echo "Website written to $OUT"
