#!/usr/bin/env bash

set -e

BEST_CLANG_CANDIDATE=""

die() {
    >&2 echo "die: $*"
    exit 1
}

pick_clang() {
    local BEST_VERSION=0
    for CLANG_CANDIDATE in clang clang-13 clang-14 /usr/local/bin/clang-13 /usr/local/bin/clang-14; do
        if ! command -v $CLANG_CANDIDATE >/dev/null 2>&1; then
            continue
        fi
        if $CLANG_CANDIDATE --version 2>&1 | grep "Apple clang" >/dev/null; then
            continue
        fi
        if ! $CLANG_CANDIDATE -dumpversion >/dev/null 2>&1; then
            continue
        fi
        local VERSION=""
        VERSION="$($CLANG_CANDIDATE -dumpversion)"
        local MAJOR_VERSION="${VERSION%%.*}"
        if [ "$MAJOR_VERSION" -gt "$BEST_VERSION" ]; then
            BEST_VERSION=$MAJOR_VERSION
            BEST_CLANG_CANDIDATE="$CLANG_CANDIDATE"
        fi
    done
    if [ "$BEST_VERSION" -lt 13 ]; then
        die "Please make sure that Clang version 13 or higher is installed."
    fi
}

# Save flags for oss-fuzz to avoid fuzzing Tools/
# https://google.github.io/oss-fuzz/getting-started/new-project-guide/#temporarily-disabling-code-instrumentation-during-builds
CFLAGS_SAVE="$CFLAGS"
CXXFLAGS_SAVE="$CXXFLAGS"
unset CFLAGS
unset CXXFLAGS
export AFL_NOOPT=1

# FIXME: Replace these CMake invocations with a CMake superbuild?
echo "Building Lagom Tools..."
cmake -GNinja -B Build/tools \
    -DBUILD_LAGOM=OFF \
    -DCMAKE_INSTALL_PREFIX=Build/tool-install
ninja -C Build/tools install

# Restore flags for oss-fuzz
export CFLAGS="${CFLAGS_SAVE}"
export CXXFLAGS="${CXXFLAGS_SAVE}"
unset AFL_NOOPT

echo "Building Lagom Fuzzers..."

if [ "$#" -gt "0" ] && [ "--oss-fuzz" = "$1" ] ; then
    echo "Building for oss-fuzz configuration..."
    cmake -GNinja -B Build/fuzzers \
        -DBUILD_LAGOM=ON \
        -DBUILD_SHARED_LIBS=OFF \
        -DENABLE_OSS_FUZZ=ON \
        -DCMAKE_C_COMPILER="$CC" \
        -DCMAKE_CXX_COMPILER="$CXX" \
        -DCMAKE_CXX_FLAGS="$CXXFLAGS -DOSS_FUZZ=ON" \
        -DLINKER_FLAGS="$LIB_FUZZING_ENGINE" \
        -DCMAKE_PREFIX_PATH=Build/tool-install
    ninja -C Build/fuzzers
    cp Build/fuzzers/Fuzzers/Fuzz* "$OUT"/
else
    echo "Building for local fuzz configuration..."
    pick_clang
    cmake -GNinja -B Build/lagom-fuzzers \
        -DBUILD_LAGOM=ON \
        -DENABLE_FUZZER_SANITIZER=ON \
        -DENABLE_ADDRESS_SANITIZER=ON \
        -DENABLE_UNDEFINED_SANITIZER=ON \
        -DCMAKE_PREFIX_PATH=Build/tool-install \
        -DCMAKE_C_COMPILER=$BEST_CLANG_CANDIDATE \
        -DCMAKE_CXX_COMPILER="${BEST_CLANG_CANDIDATE/clang/clang++}"
    ninja -C Build/lagom-fuzzers
fi
