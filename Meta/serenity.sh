#!/usr/bin/env bash

set -e

ARG0=$0
print_help() {
    NAME=$(basename "$ARG0")
    cat <<EOF
Usage: $NAME COMMAND [TARGET] [TOOLCHAIN] [ARGS...]
  Supported TARGETs: aarch64, x86_64, riscv64, lagom. Defaults to SERENITY_ARCH, or host architecture if not set.
  Supported TOOLCHAINs: GNU, Clang. Defaults to SERENITY_TOOLCHAIN, or GNU if not set.
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
    $NAME run x86_64 GNU smp=on
        Runs the image in QEMU passing "smp=on" to the kernel command line
    $NAME run
        Runs the image for the default TARGET x86_64 in QEMU
    $NAME run lagom js -A
        Runs the Lagom-built js(1) REPL
    $NAME test lagom
        Runs the unit tests on the build host
    $NAME kaddr2line x86_64 GNU 0x12345678
        Resolves the address 0x12345678 in the Kernel binary
    $NAME addr2line x86_64 WindowServer 0x12345678
        Resolves the address 0x12345678 in the WindowServer binary
    $NAME gdb x86_64 GNU smp=on -ex 'hb *init'
        Runs the image for the TARGET x86_64 in qemu and attaches a gdb session
        setting a breakpoint at the init() function in the Kernel.
EOF
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

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# shellcheck source=/dev/null
. "${DIR}/shell_include.sh"

exit_if_running_as_root "Do not run serenity.sh as root, your Build directory will become root-owned"

host_arch=$(uname -m)
if [ "$host_arch" = "x86_64" ] || [ "$host_arch" = "amd64" ] || [ "$host_arch" = "x64" ]; then
    host_arch="x86_64"
elif [ "$host_arch" = "aarch64" ] || [ "$host_arch" = "arm64" ]; then
    host_arch="aarch64"
elif [ "$host_arch" = "riscv64" ]; then
    host_arch="riscv64"
else
    die "Unknown host architecture: $host_arch"
fi

# shellcheck source=/dev/null
. "${DIR}/find_compiler.sh"

if [ -n "$1" ]; then
    TARGET="$1"; shift
else
    TARGET="${SERENITY_ARCH:-${host_arch}}"
fi

CMAKE_ARGS=()

# Toolchain selection only applies to non-lagom targets.
if [ "$TARGET" != "lagom" ] && [ -n "$1" ]; then
    TOOLCHAIN_TYPE="$1"; shift
else
    TOOLCHAIN_TYPE="${SERENITY_TOOLCHAIN:-"GNU"}"
fi
if ! [[ "${TOOLCHAIN_TYPE}" =~ ^(GNU|Clang)$ ]]; then
    >&2 echo "ERROR: unknown toolchain '${TOOLCHAIN_TYPE}'."
    exit 1
fi
CMAKE_ARGS+=( "-DSERENITY_TOOLCHAIN=$TOOLCHAIN_TYPE" )

CMD_ARGS=( "$@" )

get_top_dir() {
    git rev-parse --show-toplevel
}

is_valid_target() {
    if [ "$TARGET" = "aarch64" ]; then
        CMAKE_ARGS+=("-DSERENITY_ARCH=aarch64")
        return 0
    fi
    if [ "$TARGET" = "x86_64" ]; then
        CMAKE_ARGS+=("-DSERENITY_ARCH=x86_64")
        return 0
    fi
    if [ "$TARGET" = "riscv64" ]; then
        CMAKE_ARGS+=("-DSERENITY_ARCH=riscv64")
        return 0
    fi
    if [ "$TARGET" = "lagom" ]; then
        CMAKE_ARGS+=("-DBUILD_LAGOM=ON")
        if [ "${CMD_ARGS[0]}" = "ladybird" ]; then
            CMAKE_ARGS+=("-DENABLE_LAGOM_LADYBIRD=ON")
        fi
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

cmd_with_target() {
    is_valid_target || ( >&2 echo "Unknown target: $TARGET"; usage )

    pick_host_compiler
    CMAKE_ARGS+=("-DCMAKE_C_COMPILER=${CC}")
    CMAKE_ARGS+=("-DCMAKE_CXX_COMPILER=${CXX}")

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
        export SERENITY_TOOLCHAIN="$TOOLCHAIN_TYPE"
        if [ "$TOOLCHAIN_TYPE" = "Clang" ]; then
            TOOLCHAIN_DIR="$SERENITY_SOURCE_DIR/Toolchain/Local/clang"
        else
            TOOLCHAIN_DIR="$SERENITY_SOURCE_DIR/Toolchain/Local/$TARGET_TOOLCHAIN/$TARGET"
        fi
        JAKT_TOOLCHAIN_DIR="$SERENITY_SOURCE_DIR/Toolchain/Local/jakt"
        SUPER_BUILD_DIR="$SERENITY_SOURCE_DIR/Build/superbuild-$TARGET$TARGET_TOOLCHAIN"
        JAKT_LIB_DIR="$BUILD_DIR/Root/usr/local/lib/$TARGET-pc-serenity-unknown"
    else
        SUPER_BUILD_DIR="$BUILD_DIR"
        CMAKE_ARGS+=("-DCMAKE_INSTALL_PREFIX=$SERENITY_SOURCE_DIR/Build/lagom-install")
        CMAKE_ARGS+=("-DSERENITY_CACHE_DIR=${SERENITY_SOURCE_DIR}/Build/caches")
    fi
    export PATH="$SERENITY_SOURCE_DIR/Toolchain/Local/cmake/bin":$PATH
}

ensure_target() {
    [ -f "$SUPER_BUILD_DIR/build.ninja" ] || create_build_dir
}

run_tests() {
    local TEST_NAME="$1"
    local CTEST_ARGS=("--output-on-failure" "--test-dir" "$BUILD_DIR")
    if [ -n "$TEST_NAME" ]; then
        if [ "$TEST_NAME" = "WPT" ]; then
            CTEST_ARGS+=("-C" "Integration")
        fi
        CTEST_ARGS+=("-R" "$TEST_NAME")
    fi
    ctest "${CTEST_ARGS[@]}"
}

build_target() {
    if [ "$TARGET" = "lagom" ]; then
        # Ensure that all lagom binaries get built, in case user first
        # invoked superbuild for serenity target that doesn't set -DBUILD_LAGOM=ON
        local EXTRA_CMAKE_ARGS=()
        if [ "${CMD_ARGS[0]}" = "ladybird" ]; then
            EXTRA_CMAKE_ARGS=("-DENABLE_LAGOM_LADYBIRD=ON")
        fi
        cmake -S "$SERENITY_SOURCE_DIR/Meta/Lagom" -B "$BUILD_DIR" -DBUILD_LAGOM=ON "${EXTRA_CMAKE_ARGS[@]}"
    fi

    # Get either the environment MAKEJOBS or all processors via CMake
    [ -z "$MAKEJOBS" ] && MAKEJOBS=$(cmake -P "$SERENITY_SOURCE_DIR/Meta/CMake/processor-count.cmake")

    # With zero args, we are doing a standard "build"
    # With multiple args, we are doing an install/image/run
    if [ $# -eq 0 ]; then
        CMAKE_BUILD_PARALLEL_LEVEL="$MAKEJOBS" cmake --build "$SUPER_BUILD_DIR"
    else
        ninja -j "$MAKEJOBS" -C "$BUILD_DIR" -- "$@"
    fi
}

build_image() {
    if [ "$SERENITY_RUN" = "limine" ]; then
        build_target limine-image
    else
        build_target qemu-image
    fi
}

delete_target() {
    [ ! -d "$BUILD_DIR" ] || rm -rf "$BUILD_DIR"
    [ ! -d "$SUPER_BUILD_DIR" ] || rm -rf "$SUPER_BUILD_DIR"
}

build_cmake() {
    echo "CMake version too old: build_cmake"
    ( cd "$SERENITY_SOURCE_DIR/Toolchain" && ./BuildCMake.sh )
}

build_toolchain() {
    echo "build_toolchain: $TOOLCHAIN_DIR"

    if [ "$TOOLCHAIN_TYPE" = "Clang" ]; then
        ( cd "$SERENITY_SOURCE_DIR/Toolchain" && ./BuildClang.sh )
    else
        (
            # HACK: ISL's configure fails with "Link Time Optimisation is not supported" if CC is
            #       Homebrew Clang due to incompatibility with Xcode's ranlib.
            [ "$(uname -s)" = "Darwin" ] && unset CC CXX
            cd "$SERENITY_SOURCE_DIR/Toolchain" && ARCH="$TARGET" ./BuildGNU.sh
        )
    fi
}

build_jakt() {
    [ -z "$JAKT_TOOLCHAIN_DIR" ] && return
    echo "build_jakt: $JAKT_TOOLCHAIN_DIR"
    ( cd "$SERENITY_SOURCE_DIR/Toolchain" && ./BuildJakt.sh )
}

ensure_jakt() {
    if ! [ -d "$JAKT_TOOLCHAIN_DIR" ] || ! [ -d "$JAKT_LIB_DIR" ]; then
        build_jakt || true # CMake can handle the failure.
    fi
}

ensure_toolchain() {
    if [ "$(cmake -P "$SERENITY_SOURCE_DIR"/Meta/CMake/cmake-version.cmake)" -ne 1 ]; then
        build_cmake
    fi
    [ -d "$TOOLCHAIN_DIR" ] || build_toolchain

    if [ "$TOOLCHAIN_TYPE" = "GNU" ]; then
        local ld_version
        ld_version="$("$TOOLCHAIN_DIR"/bin/"$TARGET"-pc-serenity-ld -v)"
        local expected_version="GNU ld (GNU Binutils) 2.41"
        if [ "$ld_version" != "$expected_version" ]; then
            echo "Your toolchain has an old version of binutils installed."
            echo "    installed version: \"$ld_version\""
            echo "    expected version:  \"$expected_version\""
            echo "Please run $ARG0 rebuild-toolchain $TARGET to update it."
            exit 1
        fi
    fi

    ensure_jakt
}

confirm_rebuild_if_toolchain_exists() {
    [ ! -d "$TOOLCHAIN_DIR" ] && return

    read -rp "You already have a toolchain, are you sure you want to delete and rebuild one [y/N]? " input

    if [[ "$input" != "y" && "$input" != "Y" ]]; then
        die "Aborted rebuild"
    fi
}

delete_toolchain() {
    [ ! -d "$TOOLCHAIN_DIR" ] || rm -rf "$TOOLCHAIN_DIR"
    ! [ -d "$JAKT_TOOLCHAIN_DIR" ] || rm -fr "$JAKT_TOOLCHAIN_DIR"
}

kill_tmux_session() {
    if [ -n "$TMUX_SESSION" ]; then
        tmux has-session -t "$TMUX_SESSION" >/dev/null 2>&1 && tmux kill-session -t "$TMUX_SESSION"
    fi
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
                if [ "$LAGOM_EXECUTABLE" = "ladybird" ]; then
                    # FIXME: Make ladybird less cwd-dependent while in the build directory
                    cd "$BUILD_DIR/Ladybird"
                fi
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
        gdb "$BUILD_DIR/bin/$LAGOM_EXECUTABLE" "${GDB_ARGS[@]}"
    else
        if [ -n "$KERNEL_CMD_LINE" ]; then
            export SERENITY_KERNEL_CMDLINE="$KERNEL_CMD_LINE"
        fi
        sleep 1
        "$(get_top_dir)/Meta/debug-kernel.sh" "${GDB_ARGS[@]}"
    fi
}

build_and_run_lagom_target() {
    local run_target="${1}"
    local lagom_target="${CMD_ARGS[0]}"
    local lagom_args

    # All command arguments must have any existing semicolon escaped, to prevent CMake from
    # interpreting them as list separators.
    local cmd_args=()
    for arg in "${CMD_ARGS[@]:1}"; do
        cmd_args+=( "${arg//;/\\;}" )
    done

    # Then existing list separators must be replaced with a semicolon for CMake.
    lagom_args=$(IFS=';' ; echo -e "${cmd_args[*]}")

    LAGOM_TARGET="${lagom_target}" LAGOM_ARGS="${lagom_args[*]}" build_target "${run_target}"
}

if [[ "$CMD" =~ ^(build|install|image|copy-src|run|gdb|test|rebuild|recreate|kaddr2line|addr2line|setup-and-run)$ ]]; then
    cmd_with_target
    [[ "$CMD" != "recreate" && "$CMD" != "rebuild" ]] || delete_target
    [ "$TARGET" = "lagom" ] || ensure_toolchain
    ensure_jakt
    ensure_target
    case "$CMD" in
        build)
            build_target "${CMD_ARGS[@]}"
            ;;
        install)
            build_target
            build_target install
            ;;
        image)
            lagom_unsupported
            build_target
            build_target install
            build_image
            ;;
        copy-src)
          lagom_unsupported
          build_target
          build_target install
          export SERENITY_COPY_SOURCE=1
          build_image
          ;;
        run)
            if [ "$TARGET" = "lagom" ]; then
                if [ "${CMD_ARGS[0]}" = "ladybird" ]; then
                    build_and_run_lagom_target "run-ladybird"
                else
                    build_and_run_lagom_target "run-lagom-target"
                fi
            else
                build_target
                build_target install
                build_image
                if [ -n "${CMD_ARGS[0]}" ]; then
                    export SERENITY_KERNEL_CMDLINE="${CMD_ARGS[0]}"
                fi
                build_target run
            fi
            ;;
        gdb)
            if [ "$TARGET" = "lagom" ]; then
                [ $# -ge 1 ] || usage
                build_target "${CMD_ARGS[@]}"
                run_gdb "${CMD_ARGS[@]}"
            else
                command -v tmux >/dev/null 2>&1 || die "Please install tmux!"
                build_target
                build_target install
                build_image

                TMUX_SESSION="tmux-serenity-gdb-$(date +%s)"
                tmux new-session -e "TMUX_SESSION=$TMUX_SESSION" -s "$TMUX_SESSION" "$ARG0" __tmux_cmd "$TARGET" "$TOOLCHAIN_TYPE" run "${CMD_ARGS[@]}" \; set-option -t 0 mouse on \; split-window -e "TMUX_SESSION=$TMUX_SESSION" "$ARG0" __tmux_cmd "$TARGET" "$TOOLCHAIN_TYPE" gdb "${CMD_ARGS[@]}" \;
            fi
            ;;
        test)
            build_target
            if [ "$TARGET" = "lagom" ]; then
                run_tests "${CMD_ARGS[0]}"
            else
                build_target install
                build_image
                # In contrast to CI, we don't set 'panic=shutdown' here,
                # in case the user wants to inspect qemu some more.
                export SERENITY_KERNEL_CMDLINE="graphics_subsystem_mode=off system_mode=self-test"
                export SERENITY_RUN="ci"
                build_target run
            fi
            ;;
        rebuild)
            build_target "${CMD_ARGS[@]}"
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
                ADDR2LINE="$TOOLCHAIN_DIR/bin/$TARGET-pc-serenity-addr2line"
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
                ADDR2LINE="$TOOLCHAIN_DIR/bin/$TARGET-pc-serenity-addr2line"
            fi
            if [ -x "$BINARY_FILE_PATH" ]; then
                "$ADDR2LINE" -e "$BINARY_FILE_PATH" "$@"
            else
                find "$BUILD_DIR" -name "$BINARY_FILE" -executable -type f -exec "$ADDR2LINE" -e {} "$@" \;
            fi
            ;;
        *)
            build_target "$CMD" "${CMD_ARGS[@]}"
            ;;
    esac
elif [ "$CMD" = "delete" ]; then
    cmd_with_target
    delete_target
elif [ "$CMD" = "rebuild-toolchain" ]; then
    cmd_with_target
    lagom_unsupported "The lagom target uses the host toolchain"
    confirm_rebuild_if_toolchain_exists
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
        export SERENITY_EXTRA_QEMU_ARGS="${SERENITY_EXTRA_QEMU_ARGS} -no-reboot -no-shutdown -S"
        # We need to disable kaslr to let gdb map the kernel symbols correctly
        export SERENITY_KERNEL_CMDLINE="${SERENITY_KERNEL_CMDLINE} disable_kaslr"
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
