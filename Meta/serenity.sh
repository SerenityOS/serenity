#!/usr/bin/env bash

set -e

ARG0=$0
print_help() {
    NAME=$(basename "$ARG0")
    cat <<EOF
Usage: $NAME COMMAND [TARGET] [TOOLCHAIN] [ARGS...]
  Supported TARGETs: aarch64, i686, x86_64, lagom. Defaults to SERENITY_ARCH, or i686 if not set.
  Supported TOOLCHAINs: GNU, Clang. Defaults to GNU if not set.
  Supported COMMANDs:
    build:      Compiles the target binaries, [ARGS...] are passed through to ninja
    install:    Installs the target binary
    image:      Creates a disk image with the installed binaries
    run:        TARGET lagom: $NAME run lagom LAGOM_EXECUTABLE [ARGS...]
                    Runs the Lagom-built LAGOM_EXECUTABLE on the build host, e.g.
                    'shell' or 'js', [ARGS...] are passed through to the executable
                All other TARGETs: $NAME run [TARGET] [KERNEL_CMD_LINE]
                    Runs the built image in QEMU, and optionally passes the
                    KERNEL_CMD_LINE to the Kernel
    gdb:        Same as run, but also starts a gdb remote session.
                TARGET lagom: $NAME gdb lagom LAGOM_EXECUTABLE [-ex 'any gdb command']...
                    Passes through '-ex' commands to gdb
                All other TARGETs: $NAME gdb [TARGET] [KERNEL_CMD_LINE] [-ex 'any gdb command']...
                    If specified, passes the KERNEL_CMD_LINE to the Kernel
                    Passes through '-ex' commands to gdb
    test:       TARGET lagom: $NAME test lagom [TEST_NAME_PATTERN]
                    Runs the unit tests on the build host, or if TEST_NAME_PATTERN
                    is specified tests matching it.
                All other TARGETs: $NAME test [TARGET]
                    Runs the built image in QEMU in self-test mode, by passing
                    system_mode=self-test to the Kernel
    delete:     Removes the build environment for TARGET
    recreate:   Deletes and re-creates the build environment for TARGET
    rebuild:    Deletes and re-creates the build environment, and compiles for TARGET
    kaddr2line: $NAME kaddr2line TARGET ADDRESS
                    Resolves the ADDRESS in the Kernel/Kernel binary to a file:line
    addr2line:  $NAME addr2line TARGET BINARY_FILE ADDRESS
                    Resolves the ADDRESS in BINARY_FILE to a file:line. It will
                    attempt to find the BINARY_FILE in the appropriate build directory
    rebuild-toolchain: Deletes and re-builds the TARGET's toolchain
    rebuild-world:     Deletes and re-builds the toolchain and build environment for TARGET.
    copy-src:   Same as image, but also copies the project's source tree to ~/Source/serenity
                in the built disk image.


  Examples:
    $NAME run i686 smp=on
        Runs the image in QEMU passing "smp=on" to the kernel command line
    $NAME run
        Runs the image for the default TARGET i686 in QEMU
    $NAME run lagom js -A
        Runs the Lagom-built js(1) REPL
    $NAME test lagom
        Runs the unit tests on the build host
    $NAME kaddr2line i686 0x12345678
        Resolves the address 0x12345678 in the Kernel binary
    $NAME addr2line i686 WindowServer 0x12345678
        Resolves the address 0x12345678 in the WindowServer binary
    $NAME gdb i686 smp=on -ex 'hb *init'
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
    TARGET="${SERENITY_ARCH:-"i686"}"
fi

CMAKE_ARGS=()

# Toolchain selection only applies to non-lagom targets.
if [ "$TARGET" != "lagom" ]; then
    case "$1" in
        GNU|Clang)
            TOOLCHAIN_TYPE="$1"; shift
            ;;
        *)
            if [ -n "$1" ]; then
                echo "WARNING: unknown toolchain '$1'. Defaulting to GNU."
                echo "         Valid values are 'Clang', 'GNU' (default)"
            fi
            TOOLCHAIN_TYPE="GNU"
            ;;
    esac
    CMAKE_ARGS+=( "-DSERENITY_TOOLCHAIN=$TOOLCHAIN_TYPE" )
fi

CMD_ARGS=( "$@" )

get_top_dir() {
    git rev-parse --show-toplevel
}

is_valid_target() {
    if [ "$TARGET" = "aarch64" ]; then
        CMAKE_ARGS+=("-DSERENITY_ARCH=aarch64")
        return 0
    fi
    if [ "$TARGET" = "i686" ]; then
        CMAKE_ARGS+=("-DSERENITY_ARCH=i686")
        return 0
    fi
    if [ "$TARGET" = "x86_64" ]; then
        CMAKE_ARGS+=("-DSERENITY_ARCH=x86_64")
        return 0
    fi
    if [ "$TARGET" = "lagom" ]; then
        CMAKE_ARGS+=("-DBUILD_LAGOM=ON")
        return 0
    fi
    return 1
}

create_build_dir() {
    if [ "$TARGET" != "lagom" ]; then
        cmake -GNinja "${CMAKE_ARGS[@]}" -S "$SERENITY_SOURCE_DIR/Meta/CMake/Superbuild" -B "$SUPER_BUILD_DIR"
    else
        cmake -GNinja "${CMAKE_ARGS[@]}" -S "$SERENITY_SOURCE_DIR/Meta/Lagom" -B "$SUPER_BUILD_DIR"
    fi
}

pick_gcc() {
    local BEST_VERSION=0
    local BEST_GCC_CANDIDATE=""
    for GCC_CANDIDATE in gcc gcc-10 gcc-11 gcc-12 /usr/local/bin/gcc-11 /opt/homebrew/bin/gcc-11; do
        if ! command -v $GCC_CANDIDATE >/dev/null 2>&1; then
            continue
        fi
        if $GCC_CANDIDATE --version 2>&1 | grep "Apple clang" >/dev/null; then
            continue
        fi
        if ! $GCC_CANDIDATE -dumpversion >/dev/null 2>&1; then
            continue
        fi
        local VERSION=""
        VERSION="$($GCC_CANDIDATE -dumpversion)"
        local MAJOR_VERSION="${VERSION%%.*}"
        if [ "$MAJOR_VERSION" -gt "$BEST_VERSION" ]; then
            BEST_VERSION=$MAJOR_VERSION
            BEST_GCC_CANDIDATE="$GCC_CANDIDATE"
        fi
    done
    CMAKE_ARGS+=("-DCMAKE_C_COMPILER=$BEST_GCC_CANDIDATE")
    CMAKE_ARGS+=("-DCMAKE_CXX_COMPILER=${BEST_GCC_CANDIDATE/gcc/g++}")
    if [ "$BEST_VERSION" -lt 10 ]; then
        die "Please make sure that GCC version 10.2 or higher is installed."
    fi
}

cmd_with_target() {
    is_valid_target || ( >&2 echo "Unknown target: $TARGET"; usage )
    pick_gcc

    if [ ! -d "$SERENITY_SOURCE_DIR" ]; then
        SERENITY_SOURCE_DIR="$(get_top_dir)"
        export SERENITY_SOURCE_DIR
    fi
    local TARGET_TOOLCHAIN=""
    if [[ "$TOOLCHAIN_TYPE" != "GNU" && "$TARGET" != "lagom" ]]; then
        # Only append the toolchain if it's not GNU
        TARGET_TOOLCHAIN=$(echo "$TOOLCHAIN_TYPE" | tr "[:upper:]" "[:lower:]")
    fi
    BUILD_DIR="$SERENITY_SOURCE_DIR/Build/$TARGET$TARGET_TOOLCHAIN"
    if [ "$TARGET" != "lagom" ]; then
        export SERENITY_ARCH="$TARGET"
        if [ "$TOOLCHAIN_TYPE" = "Clang" ]; then
            TOOLCHAIN_DIR="$SERENITY_SOURCE_DIR/Toolchain/Local/clang"
        else
            TOOLCHAIN_DIR="$SERENITY_SOURCE_DIR/Toolchain/Local/$TARGET_TOOLCHAIN/$TARGET"
        fi
        SUPER_BUILD_DIR="$SERENITY_SOURCE_DIR/Build/superbuild-$TARGET$TARGET_TOOLCHAIN"
    else
        SUPER_BUILD_DIR="$BUILD_DIR"
        CMAKE_ARGS+=("-DCMAKE_INSTALL_PREFIX=$SERENITY_SOURCE_DIR/Build/lagom-install")
    fi
}

ensure_target() {
    [ -f "$SUPER_BUILD_DIR/build.ninja" ] || create_build_dir
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
    if [ "$TARGET" = "lagom" ]; then
        # Ensure that all lagom binaries get built, in case user first
        # invoked superbuild for serenity target that doesn't set -DBUILD_LAGOM=ON
        cmake -S "$SERENITY_SOURCE_DIR/Meta/Lagom" -B "$BUILD_DIR" -DBUILD_LAGOM=ON
    fi
    # With zero args, we are doing a standard "build"
    # With multiple args, we are doing an install/image/run
    if [ $# -eq 0 ]; then
        cmake --build "$SUPER_BUILD_DIR"
    else
        ninja -C "$BUILD_DIR" -- "$@"
    fi
}

delete_target() {
    [ ! -d "$BUILD_DIR" ] || rm -rf "$BUILD_DIR"
    [ ! -d "$SUPER_BUILD_DIR" ] || rm -rf "$SUPER_BUILD_DIR"
}

build_toolchain() {
    echo "build_toolchain: $TOOLCHAIN_DIR"
    if [ "$TOOLCHAIN_TYPE" = "Clang" ]; then
        ( cd "$SERENITY_SOURCE_DIR/Toolchain" && ./BuildClang.sh )
    else
        ( cd "$SERENITY_SOURCE_DIR/Toolchain" && ARCH="$TARGET" ./BuildIt.sh )
    fi
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
    local LAGOM_EXECUTABLE=""
    for arg in "${CMD_ARGS[@]}"; do
        if [ "$PASS_ARG_TO_GDB" != "" ]; then
            GDB_ARGS+=( "$PASS_ARG_TO_GDB" "$arg" )
            PASS_ARG_TO_GDB=""
        elif [ "$arg" = "-ex" ]; then
            PASS_ARG_TO_GDB="$arg"
        elif [[ "$arg" =~ ^-.*$ ]]; then
            die "Don't know how to handle argument: $arg"
        else
            if [ "$TARGET" = "lagom" ]; then
                if [ "$LAGOM_EXECUTABLE" != "" ]; then
                    die "Lagom executable can't be specified more than once"
                fi
                LAGOM_EXECUTABLE="$arg"
            else
                if [ "$KERNEL_CMD_LINE" != "" ]; then
                    die "Kernel command line can't be specified more than once"
                fi
                KERNEL_CMD_LINE="$arg"
            fi
        fi
    done
    if [ "$PASS_ARG_TO_GDB" != "" ]; then
        GDB_ARGS+=( "$PASS_ARG_TO_GDB" )
    fi
    if [ "$TARGET" = "lagom" ]; then
        gdb "$BUILD_DIR/$LAGOM_EXECUTABLE" "${GDB_ARGS[@]}"
    else
        if [ -n "$KERNEL_CMD_LINE" ]; then
            export SERENITY_KERNEL_CMDLINE="$KERNEL_CMD_LINE"
        fi
        sleep 1
        "$(get_top_dir)/Meta/debug-kernel.sh" "${GDB_ARGS[@]}"
    fi
}

if [[ "$CMD" =~ ^(build|install|image|copy-src|run|gdb|test|rebuild|recreate|kaddr2line|addr2line|setup-and-run)$ ]]; then
    cmd_with_target
    [[ "$CMD" != "recreate" && "$CMD" != "rebuild" ]] || delete_target
    [ "$TARGET" = "lagom" ] || ensure_toolchain
    ensure_target
    case "$CMD" in
        build)
            build_target "$@"
            ;;
        install)
            build_target
            build_target install
            ;;
        image)
            lagom_unsupported
            build_target
            build_target install
            build_target image
            ;;
        copy-src)
          lagom_unsupported
          build_target
          build_target install
          export SERENITY_COPY_SOURCE=1
          build_target image
          ;;
        run)
            if [ "$TARGET" = "lagom" ]; then
                build_target "${CMD_ARGS[0]}"
                "$BUILD_DIR/${CMD_ARGS[0]}" "${CMD_ARGS[@]:1}"
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
            if [ "$TARGET" = "lagom" ]; then
                [ $# -ge 1 ] || usage
                build_target "$@"
                run_gdb "${CMD_ARGS[@]}"
            else
                build_target
                build_target install
                build_target image
                tmux new-session "$ARG0" __tmux_cmd "$TARGET" run "${CMD_ARGS[@]}" \; set-option -t 0 mouse on \; split-window "$ARG0" __tmux_cmd "$TARGET" gdb "${CMD_ARGS[@]}" \;
            fi
            ;;
        test)
            build_target
            if [ "$TARGET" = "lagom" ]; then
                run_tests "${CMD_ARGS[0]}"
            else
                build_target install
                build_target image
                # In contrast to CI, we don't set 'panic=shutdown' here,
                # in case the user wants to inspect qemu some more.
                export SERENITY_KERNEL_CMDLINE="fbdev=off system_mode=self-test"
                export SERENITY_RUN="ci"
                build_target run
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
            if [ "$TOOLCHAIN_TYPE" = "Clang" ]; then
                ADDR2LINE="$TOOLCHAIN_DIR/bin/llvm-addr2line"
            else
                ADDR2LINE="$TOOLCHAIN_DIR/binutils/binutils/addr2line"
            fi
            "$ADDR2LINE" -e "$BUILD_DIR/Kernel/Kernel" "$@"
            ;;
        addr2line)
            build_target
            [ $# -ge 2 ] || usage
            BINARY_FILE="$1"; shift
            BINARY_FILE_PATH="$BUILD_DIR/$BINARY_FILE"
            if [ "$TARGET" = "lagom" ]; then
                command -v addr2line >/dev/null 2>&1 || die "Please install addr2line!"
                ADDR2LINE=addr2line
            elif [ "$TOOLCHAIN_TYPE" = "Clang" ]; then
                ADDR2LINE="$TOOLCHAIN_DIR/bin/llvm-addr2line"
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
elif [ "$CMD" = "rebuild-world" ]; then
    cmd_with_target
    lagom_unsupported "The lagom target uses the host toolchain"
    delete_toolchain
    delete_target
    ensure_toolchain
    ensure_target
    build_target
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
