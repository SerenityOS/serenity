#!/usr/bin/env bash

set -e

SCRIPT_PATH="$(dirname "${0}")"
cd "${SCRIPT_PATH}"

BEST_CLANG_CANDIDATE=""

die() {
    >&2 echo "die: $*"
    exit 1
}

pick_clang() {
    local BEST_VERSION=0
    for CLANG_CANDIDATE in clang clang-17 clang-18 /opt/homebrew/opt/llvm/bin/clang ; do
        if ! command -v $CLANG_CANDIDATE >/dev/null 2>&1; then
            continue
        fi
        if $CLANG_CANDIDATE --version 2>&1 | grep "Apple clang" >/dev/null; then
            echo "Skipping Apple clang, as Apple does not ship libfuzzer with Xcode..."
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
    if [ "$BEST_VERSION" -lt 15 ]; then
        die "Please make sure that Clang version 15 or higher is installed."
    fi
}

# Save flags for oss-fuzz to avoid fuzzing Tools/
# https://google.github.io/oss-fuzz/getting-started/new-project-guide/#temporarily-disabling-code-instrumentation-during-builds
CFLAGS_SAVE="$CFLAGS"
CXXFLAGS_SAVE="$CXXFLAGS"
unset CFLAGS
unset CXXFLAGS
export AFL_NOOPT=1

if [ "$#" -gt "0" ] && [ "--oss-fuzz" = "$1" ] ; then
    # The oss-fuzz runner image (as of June 2024) has libstdc++ 9, which lacks the
    # <coroutine> header. Use the more modern libc++ shipped with oss-fuzz's Clang.
    # It's already set in CXXFLAGS_SAVE.
    CXXFLAGS="$CXXFLAGS -DOSS_FUZZ=ON -stdlib=libc++"
fi

# FIXME: Replace these CMake invocations with a CMake superbuild?
echo "Building Lagom Tools..."
cmake -GNinja -B Build/tools \
    -DBUILD_LAGOM=OFF \
    -DCMAKE_CXX_FLAGS="$CXXFLAGS" \
    -DCMAKE_INSTALL_PREFIX=Build/tool-install \
    -Dpackage=LagomTools
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
        -DENABLE_FUZZERS_OSSFUZZ=ON \
        -DFUZZER_DICTIONARY_DIRECTORY="$OUT" \
        -DCMAKE_C_COMPILER="$CC" \
        -DCMAKE_CXX_COMPILER="$CXX" \
        -DCMAKE_CXX_FLAGS="$CXXFLAGS -DOSS_FUZZ=ON" \
        -DLINKER_FLAGS="$LIB_FUZZING_ENGINE" \
        -DCMAKE_PREFIX_PATH=Build/tool-install
    ninja -C Build/fuzzers
    cp Build/fuzzers/bin/Fuzz* "$OUT"/
elif [ "$#" -gt "0" ] && [ "--standalone" = "$1" ] ; then
    echo "Building for standalone fuzz configuration..."
    cmake -GNinja -B Build/lagom-fuzzers-standalone \
        -DBUILD_LAGOM=ON \
        -DENABLE_FUZZERS=ON \
        -DCMAKE_PREFIX_PATH=Build/tool-install
    ninja -C Build/lagom-fuzzers-standalone
else
    echo "Building for local fuzz configuration..."
    pick_clang
    cmake -GNinja -B Build/lagom-fuzzers \
        -DBUILD_LAGOM=ON \
        -DENABLE_FUZZERS_LIBFUZZER=ON \
        -DENABLE_ADDRESS_SANITIZER=ON \
        -DENABLE_UNDEFINED_SANITIZER=ON \
        -DCMAKE_PREFIX_PATH=Build/tool-install \
        -DCMAKE_C_COMPILER=$BEST_CLANG_CANDIDATE \
        -DCMAKE_CXX_COMPILER="${BEST_CLANG_CANDIDATE/clang/clang++}"
    ninja -C Build/lagom-fuzzers
fi
