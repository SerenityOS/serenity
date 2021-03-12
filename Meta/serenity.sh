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
    run:        TARGET lagom: $ARG0 run lagom LAGOM_EXECUTABLE
                    Runs the Lagom-built LAGOM_EXECUTABLE on the build host,
                    e.g. 'shell' or 'js'
                All other TARGETs: $ARG0 run [TARGET] [KERNEL_CMD_LINE]
                    Runs the built image in QEMU, and optionally passes the
                    KERNEL_CMD_LINE to the Kernel
    gdb:        Same as run, but also starts a gdb remote session.
                $ARG0 gdb [TARGET] [kernel_cmd_line] [-ex 'any gdb command']...
                    If specified, passes the kernel_cmd_line to the Kernel
                    Passes through '-ex' commands to gdb
    delete:     Removes the build environment for TARGET
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
    $ARG0 run lagom js
        Runs the Lagom-built js(1) REPL
    $ARG0 kaddr2line i686 0x12345678
        Resolves the address 0x12345678 in the Kernel binary
    $ARG0 addr2line i686 WindowServer 0x12345678
        Resolves the address 0x12345678 in the WindowServer binary
    $ARG0 gdb i686 smp=on -ex 'hb *init'
        Runs the image for the TARGET i686 in qemu and attaches a gdb session
        setting a breakpoint at the init() function in the Kernel.
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
    if [ "$TARGET" != "lagom" ]; then
        export SERENITY_ARCH="$TARGET"
        TOOLCHAIN_DIR="$SERENITY_ROOT/Toolchain/Build/$TARGET"
    fi
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
    ( cd Toolchain && ARCH="$TARGET" ./BuildIt.sh )
}

ensure_toolchain() {
    [ -d "$TOOLCHAIN_DIR" ] || build_toolchain
}

delete_toolchain() {
    [ ! -d "$TOOLCHAIN_DIR" ] || rm -rf "$TOOLCHAIN_DIR"
}

kill_tmux_session() {
    local TMUX_SESSION
    TMUX_SESSION="$(tmux display-message -p '#S')"
    [ -z "$TMUX_SESSION" ] || tmux kill-session -t "$TMUX_SESSION"
}

set_tmux_title() {
    printf "\033]2;%s\033\\" "$1"
}

lagom_unsupported() {
    [ "$TARGET" != "lagom" ] || die "${1:-"Command '$CMD' not supported for the lagom target"}"
}

run_gdb() {
    local GDB_ARGS=()
    local PASS_ARG_TO_GDB=""
    local KERNEL_CMD_LINE=""
    for arg in "${CMD_ARGS[@]}"; do
        if [ "$PASS_ARG_TO_GDB" != "" ]; then
            GDB_ARGS+=( "$PASS_ARG_TO_GDB" "$arg" )
            PASS_ARG_TO_GDB=""
        elif [ "$arg" = "-ex" ]; then
            PASS_ARG_TO_GDB="$arg"
        elif [[ "$arg" =~ ^-.*$ ]]; then
            die "Don't know how to handle argument: $arg"
        else
            if [ "$KERNEL_CMD_LINE" != "" ]; then
                die "Kernel command line can't be specified more than once"
            fi
            KERNEL_CMD_LINE="$arg"
        fi
    done
    if [ "$PASS_ARG_TO_GDB" != "" ]; then
        GDB_ARGS+=( "$PASS_ARG_TO_GDB" )
    fi
    if [ -n "$KERNEL_CMD_LINE" ]; then
        export SERENITY_KERNEL_CMDLINE="$KERNEL_CMD_LINE"
    fi
    gdb "$BUILD_DIR/Kernel/Kernel" -ex 'target remote :1234' "${GDB_ARGS[@]}" -ex cont
}

if [[ "$CMD" =~ ^(build|install|image|run|gdb|rebuild|recreate|kaddr2line|addr2line|setup-and-run)$ ]]; then
    cmd_with_target
    [[ "$CMD" != "recreate" && "$CMD" != "rebuild" ]] || delete_target
    # FIXME: We should probably call ensure_toolchain first, but this somehow causes
    # this error after the toolchain finished building:
    # ninja: error: loading 'build.ninja': No such file or directory
    ensure_target
    [ "$TARGET" = "lagom" ] || ensure_toolchain
    case "$CMD" in
        build)
            build_target "$@"
            ;;
        install)
            lagom_unsupported
            build_target
            build_target install
            ;;
        image)
            lagom_unsupported
            build_target
            build_target install
            build_target image
            ;;
        run)
            if [ "$TARGET" = "lagom" ]; then
                build_target "$@"
                "$BUILD_DIR/Meta/Lagom/${CMD_ARGS[0]}"
            else
                build_target
                build_target install
                build_target image
                if [ -n "${CMD_ARGS[0]}" ]; then
                    export SERENITY_KERNEL_CMDLINE="${CMD_ARGS[0]}"
                fi
                build_target run
            fi
            ;;
        gdb)
            command -v tmux >/dev/null 2>&1 || die "Please install tmux!"
            build_target
            if [ "$TARGET" = "lagom" ]; then
                run_tests "${CMD_ARGS[0]}"
            else
                build_target install
                build_target image
                tmux new-session "$ARG0" __tmux_cmd "$TARGET" run "${CMD_ARGS[@]}" \; set-option -t 0 mouse on \; split-window "$ARG0" __tmux_cmd "$TARGET" gdb "${CMD_ARGS[@]}" \;
            fi
            ;;
        rebuild)
            build_target "$@"
            ;;
        recreate)
            ;;
        kaddr2line)
            lagom_unsupported
            build_target
            [ $# -ge 1 ] || usage
            "$TOOLCHAIN_DIR/binutils/binutils/addr2line" -e "$BUILD_DIR/Kernel/Kernel" "$@"
            ;;
        addr2line)
            build_target
            [ $# -ge 2 ] || usage
            BINARY_FILE="$1"; shift
            BINARY_FILE_PATH="$BUILD_DIR/$BINARY_FILE"
            if [ "$TARGET" = "lagom" ]; then
                command -v addr2line >/dev/null 2>&1 || die "Please install addr2line!"
                ADDR2LINE=addr2line
            else
                ADDR2LINE="$TOOLCHAIN_DIR/binutils/binutils/addr2line"
            fi
            if [ -x "$BINARY_FILE_PATH" ]; then
                "$ADDR2LINE" -e "$BINARY_FILE_PATH" "$@"
            else
                find "$BUILD_DIR" -name "$BINARY_FILE" -executable -type f -exec "$ADDR2LINE" -e {} "$@" \;
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
    lagom_unsupported "The lagom target uses the host toolchain"
    delete_toolchain
    ensure_toolchain
elif [ "$CMD" = "__tmux_cmd" ]; then
    trap kill_tmux_session EXIT
    cmd_with_target
    CMD="$1"; shift
    CMD_ARGS=("${CMD_ARGS[@]:1}")
    if [ "$CMD" = "run" ]; then
        if [ -n "${CMD_ARGS[0]}" ]; then
            export SERENITY_KERNEL_CMDLINE="${CMD_ARGS[0]}"
        fi
        # We need to make sure qemu doesn't start until we continue in gdb
        export SERENITY_EXTRA_QEMU_ARGS="${SERENITY_EXTRA_QEMU_ARGS} -d int -no-reboot -no-shutdown -S"
        set_tmux_title 'qemu'
        build_target run
    elif [ "$CMD" = "gdb" ]; then
        set_tmux_title 'gdb'
        run_gdb "${CMD_ARGS[@]}"
    fi
else
    >&2 echo "Unknown command: $CMD"
    usage
fi
