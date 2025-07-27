# shellcheck shell=bash

HOST_COMPILER=""

is_supported_compiler() {
    local COMPILER="$1"
    if [ -z "$COMPILER" ]; then
        return 1
    fi

    local VERSION=""
    VERSION="$($COMPILER -dumpversion 2> /dev/null)" || return 1
    local MAJOR_VERSION=""
    MAJOR_VERSION="${VERSION%%.*}"
    if $COMPILER --version 2>&1 | grep "Apple clang" >/dev/null; then
        # Apple Clang version check
        BUILD_VERSION=$(echo | $COMPILER -dM -E - | grep __apple_build_version__ | cut -d ' ' -f3)
        # Xcode 14.3, based on upstream LLVM 15
        [ "$BUILD_VERSION" -ge 14030022 ] && return 0
    elif $COMPILER --version 2>&1 | grep "clang" >/dev/null; then
        # Clang version check
        [ "$MAJOR_VERSION" -ge 17 ] && return 0
    else
        # GCC version check
        [ "$MAJOR_VERSION" -ge 13 ] && return 0
    fi
    return 1
}

find_newest_compiler() {
    COMPILER_NAME="$1"
    if [ -z "$COMPILER_NAME" ]; then
        echo "Error: No compiler name provided."
        return 1
    fi

    shift

    BEST_VERSION=0
    BEST_COMPILER=""

    COMPILER_CANDIDATES=""
    if command -v "$COMPILER_NAME" >/dev/null 2>&1; then
        COMPILER_CANDIDATES="$COMPILER_NAME"
    fi

    if [ $# -gt 0 ]; then
        COMPILER_CANDIDATES="$COMPILER_CANDIDATES $*"
    fi

    for DIR in $(echo "$PATH" | tr ':' ' '); do
        for BIN in "$DIR"/"${COMPILER_NAME}"-[0-9]*; do
            [ -x "$BIN" ] && COMPILER_CANDIDATES="$COMPILER_CANDIDATES $BIN"
        done
    done

    for COMPILER in $COMPILER_CANDIDATES; do
        [ -x "$COMPILER" ] || continue
        VERSION="$($COMPILER -dumpversion 2>/dev/null || echo 0)"
        MAJOR_VERSION="${VERSION%%.*}"

        case "$MAJOR_VERSION" in
            ''|*[!0-9]*) continue ;;
        esac

        if [ "$MAJOR_VERSION" -gt "$BEST_VERSION" ]; then
            BEST_VERSION="$MAJOR_VERSION"
            BEST_COMPILER="$COMPILER"
        fi
    done

    if [ -z "$BEST_COMPILER" ]; then
        echo "No suitable $COMPILER_NAME compiler found."
        return 1
    fi

    if ! is_supported_compiler "$BEST_COMPILER"; then
        echo "Best candidate $BEST_COMPILER is not supported."
        return 1
    fi

    HOST_COMPILER="$BEST_COMPILER"
    echo "Selected compiler: $HOST_COMPILER (version $BEST_VERSION)"
}

pick_host_compiler() {
    CC=${CC:-"cc"}
    CXX=${CXX:-"c++"}

    if is_supported_compiler "$CC" && is_supported_compiler "$CXX"; then
        return
    fi

    find_newest_compiler clang /opt/homebrew/opt/llvm/bin/clang
    if is_supported_compiler "$HOST_COMPILER"; then
        export CC="${HOST_COMPILER}"
        export CXX="${HOST_COMPILER/clang/clang++}"
        return
    fi

    find_newest_compiler egcc gcc gcc-14 gcc-13 /usr/local/bin/gcc-{14,13} /opt/homebrew/bin/gcc-{14,13}
    if is_supported_compiler "$HOST_COMPILER"; then
        export CC="${HOST_COMPILER}"
        export CXX="${HOST_COMPILER/gcc/g++}"
        return
    fi

    if [ "$(uname -s)" = "Darwin" ]; then
        die "Please make sure that Xcode 14.3, Homebrew Clang 17, or higher is installed."
    else
        die "Please make sure that GCC version 13, Clang version 17, or higher is installed."
    fi
}
