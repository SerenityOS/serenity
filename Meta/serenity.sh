#!/bin/env bash
set -e

ARG0=$0
print_help() {
    cat <<EOF
Usage: $ARG0 COMMAND [TARGET] [ARGS...]
  Supported TARGETs: i686 (default), x86_64, lagom
  Supported COMMANDs:
    build:      Compiles the target binaries, [ARGS...] are passed through to ninja
    install:    Installs the target binary
    image:      Creates a disk image with the installed binaries
    run:        TARGET lagom [specific_test]: Runs the unit tests on the build host,
                    or if specific_test is specified only this test.
                All other TARGETs: $ARG0 run [TARGET] [kernel_cmd_line]
                    Runs the built image in QEMU, and optionally passes the
                    kernel_cmd_line to the Kernel
    delete:     Removes the build environment for the TARGET
    recreate:   Deletes and re-creates the build environment for TARGET
    rebuild:    Deletes and re-creates the build environment, and compiles for TARGET
    kaddr2line: $ARG0 kaddr2line TARGET ADDRESS
                    Resolves the ADDRESS in the Kernel/Kernel binary to a file:line
    addr2line:  $ARG0 addr2line TARGET BINARY_FILE ADDRESS
                    Resolves the ADDRESS in BINARY_FILE to a file:line. It will
                    attempt to find the BINARY_FILE in the appropriate build directory

    rebuild-toolchain: Deletes and re-builds the TARGET's toolchain

  Examples:
    $ARG0 run i686 smp=on
        Runs the image in QEMU passing "smp=on" to the kernel command line
    $ARG0 run
        Runs the image for the default TARGET i686 in QEMU
    $ARG0 run lagom
        Runs the unit tests on the build host
    $ARG0 kaddr2line i686 0x12345678
        Resolves the address 0x12345678 in the Kernel binary
    $ARG0 addr2line i686 WindowServer 0x12345678
        Resolves the address 0x12345678 in the WindowServer binary
EOF
}

die() {
    >&2 echo "die: $*"
    exit 1
}

usage() {
    >&2 print_help
    exit 1
}

CMD=$1
[ -n "$CMD" ] || usage
shift
if [ "$CMD" = "help" ]; then
    print_help
    exit 0
fi

if [ -n "$1" ]; then
    TARGET="$1"; shift
else
    TARGET="i686"
fi
CMD_ARGS=( "$@" )
CMAKE_ARGS=()

get_top_dir() {
    git rev-parse --show-toplevel
}

is_valid_target() {
    if [ "$TARGET" = "lagom" ]; then
        CMAKE_ARGS+=("-DBUILD_LAGOM=ON")
        return 0
    fi
    [[ "$TARGET" =~ ^(i686|x86_64|lagom)$ ]] || return 1
}

create_build_dir() {
    mkdir -p "$BUILD_DIR"
    cmake -GNinja "${CMAKE_ARGS[@]}" -S . -B "$BUILD_DIR"
}

cmd_with_target() {
    is_valid_target || ( >&2 echo "Unknown target: $TARGET"; usage )
    SERENITY_ROOT="$(get_top_dir)"
    export SERENITY_ROOT
    BUILD_DIR="$SERENITY_ROOT/Build/$TARGET"
    if [ "$TARGET" = "lagom" ]; then
        TOOLCHAIN=i686
    else
        TOOLCHAIN="$TARGET"
    fi
    export SERENITY_ARCH="TOOLCHAIN"
    TOOLCHAIN_DIR="$SERENITY_ROOT/Toolchain/Build/$TOOLCHAIN"
}

ensure_target() {
    [ -d "$BUILD_DIR" ] || create_build_dir
}

run_tests() {
    local TEST_NAME="$1"
    export CTEST_OUTPUT_ON_FAILURE=1
    if [ -n "$TEST_NAME" ]; then
        ( cd "$BUILD_DIR" && ctest -R "$TEST_NAME" )
    else
        ( cd "$BUILD_DIR" && ctest )
    fi
}

build_target() {
    ninja -C "$BUILD_DIR" -- "$@"
}

delete_target() {
    [ ! -d "$BUILD_DIR" ] || rm -rf "$BUILD_DIR"
}

build_toolchain() {
    ( cd Toolchain && ARCH="$TOOLCHAIN" ./BuildIt.sh )
}

ensure_toolchain() {
    [ -d "$TOOLCHAIN_DIR" ] || build_toolchain
}

delete_toolchain() {
    [ ! -d "$TOOLCHAIN_DIR" ] || rm -rf "$TOOLCHAIN_DIR"
}

if [[ "$CMD" =~ ^(build|install|image|run|rebuild|recreate|kaddr2line|addr2line|setup-and-run)$ ]]; then
    cmd_with_target
    [[ "$CMD" != "recreate" && "$CMD" != "rebuild" ]] || delete_target
    # FIXME: We should probably call ensure_toolchain first, but this somehow causes
    # this error after the toolchain finished building:
    # ninja: error: loading 'build.ninja': No such file or directory
    ensure_target
    ensure_toolchain
    case "$CMD" in
        build)
            build_target "$@"
            ;;
        install)
            [ "$TARGET" != "lagom" ] || die "Command '$CMD' not supported for the lagom target"
            build_target
            build_target install
            ;;
        image)
            [ "$TARGET" != "lagom" ] || die "Command '$CMD' not supported for the lagom target"
            build_target
            build_target install
            build_target image
            ;;
        run)
            build_target
            if [ "$TARGET" = "lagom" ]; then
                run_tests "${CMD_ARGS[0]}"
            else
                build_target install
                build_target image
                if [ -n "${CMD_ARGS[0]}" ]; then
                    export SERENITY_KERNEL_CMDLINE="${CMD_ARGS[0]}"
                fi
                build_target run
            fi
            ;;
        rebuild)
            build_target "$@"
            ;;
        recreate)
            ;;
        kaddr2line)
            build_target
            [ $# -ge 1 ] || usage
            "$TOOLCHAIN_DIR/binutils/binutils/addr2line" -e "$BUILD_DIR/Kernel/Kernel" "$@"
            ;;
        addr2line)
            build_target
            [ $# -ge 2 ] || usage
            BINARY_FILE="$1"; shift
            BINARY_FILE_PATH="$BUILD_DIR/$BINARY_FILE"
            if [ -x "$BINARY_FILE_PATH" ]; then
                "$TOOLCHAIN_DIR/binutils/binutils/addr2line" -e "$BINARY_FILE_PATH" "$@"
            else
                find "$BUILD_DIR" -name "$BINARY_FILE" -executable -type f -exec "$TOOLCHAIN_DIR/binutils/binutils/addr2line" -e {} "$@" \;
            fi
            ;;
        *)
            build_target "$CMD" "$@"
            ;;
    esac
elif [ "$CMD" = "delete" ]; then
    cmd_with_target
    delete_target
elif [ "$CMD" = "rebuild-toolchain" ]; then
    cmd_with_target
    [ "$TARGET" != "lagom" ] || die "The lagom target uses the host toolchain"
    delete_toolchain
    ensure_toolchain
else
    >&2 echo "Unknown command: $CMD"
    usage
fi
