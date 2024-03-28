#!/usr/bin/env bash
set -eo pipefail
# This file will need to be run in bash, for now.

# === CONFIGURATION AND SETUP ===

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# shellcheck source=/dev/null
. "${DIR}/../Meta/shell_include.sh"

exit_if_running_as_root "Do not run BuildJakt.sh as root, your Build directory will become root-owned"

echo "$DIR"

ARCHES=("x86_64" "aarch64" "riscv64")
PREFIX="$DIR/Local/jakt"

VALID_TOOLCHAINS=()

declare -A CXX_GNU=()
declare -A CXX_CLANG=()
declare -A BUILD_GNU=()
declare -A BUILD_CLANG=()
: "${BUILD_GNU[@]}" "${BUILD_CLANG[@]}" # make shellcheck understand that these might be used.

for ARCH in "${ARCHES[@]}"; do
  TARGET="$ARCH-pc-serenity"

  BUILD_GNU["$ARCH"]="$DIR/../Build/$ARCH"
  BUILD_CLANG["$ARCH"]="$DIR/../Build/${ARCH}clang"

  CXX_GNU["$ARCH"]="$DIR/Local/$ARCH/bin/$TARGET-g++"
  CXX_CLANG["$ARCH"]="$DIR/Local/clang/bin/$TARGET-clang++"

  if [ -x "${CXX_GNU["$ARCH"]}" ]; then
      VALID_TOOLCHAINS+=("GNU;${ARCH}")
  fi

  if [ -x "${CXX_CLANG[${ARCH}]}" ]; then
      VALID_TOOLCHAINS+=("CLANG;${ARCH}")
  fi
done

if [ "${#VALID_TOOLCHAINS}" -eq 0 ]; then
    die "Need to build at least one C++ toolchain (either GNU or Clang) before BuildJakt.sh can succeed"
fi

REALPATH="realpath"
SYSTEM_NAME="$(uname -s)"

NPROC=$(get_number_of_processing_units)

if [ "$SYSTEM_NAME" = "OpenBSD" ]; then
    REALPATH="readlink -f"
    export CXX=eg++
fi

if command -v ginstall &>/dev/null; then
    INSTALL=ginstall
else
    INSTALL=install
fi

buildstep() {
    NAME=$1
    shift
    "$@" 2>&1 | sed $'s|^|\x1b[34m['"${NAME}"$']\x1b[39m |'
}

buildstep_ninja() {
    # When ninja writes to a pipe, it strips ANSI escape codes and prints one line per buildstep.
    # Instead, use NINJA_STATUS so that we get colored output from LLVM's build and fewer lines of output when running in a tty.
    # ANSI escape codes in NINJA_STATUS are slightly janky (ninja thinks that "\e[34m" needs 5 output characters instead of 5, so
    # its middle elision is slightly off; also it would happily elide the "\e39m" which messes up the output if the terminal is too
    # narrow), but it's still working better than the alternative.
    NAME=$1
    shift
    env NINJA_STATUS=$'\e[34m['"${NAME}"$']\e[39m [%f/%t] ' "$@"
}

echo PREFIX is "$PREFIX"

mkdir -p "$DIR/Tarballs"

JAKT_COMMIT_HASH="f7ab7725114538f0668c58973bab2cf52b8e121c"
JAKT_NAME="jakt-${JAKT_COMMIT_HASH}"
JAKT_TARBALL="${JAKT_NAME}.tar.gz"
JAKT_GIT_URL="https://github.com/serenityos/jakt"

# === DEPENDENCIES ===
buildstep dependencies echo "Checking whether 'ninja' is available..."
if ! command -v "${NINJA:-ninja}" >/dev/null; then
    buildstep dependencies echo "Please make sure to install Ninja (for the '${NINJA:-ninja}' tool)."
    exit 1
fi

buildstep dependencies echo "Checking whether 'cmake' is available..."
if ! command -v cmake >/dev/null; then
    buildstep dependencies echo "Please make sure to install CMake (for the 'cmake' tool)."
    exit 1
fi

buildstep dependencies echo "Checking whether your C++ compiler works..."
if ! ${CXX:-c++} -o /dev/null -xc++ - >/dev/null <<'PROGRAM'
int main() {}
PROGRAM
then
    buildstep dependencies echo "Please make sure to install a working C++ compiler."
    exit 1
fi

# === DOWNLOAD AND PATCH ===
pushd "$DIR/Tarballs"
    if [ ! -d "${JAKT_NAME}" ]; then
        echo "Downloading jakt..."
        rm -f "$JAKT_TARBALL"
        curl -L --output "$JAKT_TARBALL" "$JAKT_GIT_URL/archive/$JAKT_COMMIT_HASH.tar.gz"

        echo "Extracting jakt..."
        tar -xzf "${JAKT_TARBALL}"
    else
        echo "Using existing Jakt source directory"
    fi
popd

# === COMPILE AND INSTALL ===
rm -rf "$PREFIX"
mkdir -p "$PREFIX"

rm -rf "$DIR/Build/jakt"
mkdir -p "$DIR/Build/jakt"
pushd "$DIR/Build/jakt"
    echo "XXX configure jakt"
    buildstep "jakt/configure" cmake -S "$DIR/Tarballs/${JAKT_NAME}" -B . \
                                        -DSERENITY_SOURCE_DIR="$DIR/.."   \
                                        -DCMAKE_INSTALL_PREFIX="$PREFIX"  \
                                        -DCMAKE_BUILD_TYPE=Release        \
                                        -GNinja                           \
            || exit 1

    echo "XXX build jakt"
    buildstep_ninja "jakt/build" ninja jakt_stage1
    echo "XXX install jakt"
    buildstep_ninja "jakt/install" ninja install
popd

build_for() {
    TOOLCHAIN="$1"
    ARCH="$2"

    TARGET="$ARCH-pc-serenity"
    JAKT_TARGET="$TARGET-unknown"

    current_build="BUILD_${TOOLCHAIN}[${ARCH}]"
    current_cxx="CXX_${TOOLCHAIN}[${ARCH}]"

    BUILD="${!current_build}"
    TARGET_CXX="${!current_cxx}"

    # On at least OpenBSD, the path must exist to call realpath(3) on it
    if [ ! -d "$BUILD" ]; then
        mkdir -p "$BUILD"
    fi
    BUILD=$($REALPATH "$BUILD")
    echo "XXX building jakt support libs in $BUILD with $TARGET_CXX"

    SYSROOT="$BUILD/Root"
    echo SYSROOT is "$SYSROOT"

    rm -rf "$DIR/Build/jakt-$TOOLCHAIN-$ARCH"
    mkdir -p "$DIR/Build/jakt-$TOOLCHAIN-$ARCH"
    pushd "$DIR/Build/jakt-$TOOLCHAIN-$ARCH"
        echo "XXX serenity libc headers"
        mkdir -p "$BUILD"
        pushd "$BUILD"
            mkdir -p Root/usr/include/
            SRC_ROOT=$($REALPATH "$DIR"/..)
            FILES=$(find \
                "$SRC_ROOT"/AK \
                "$SRC_ROOT"/Kernel/API \
                "$SRC_ROOT"/Kernel/Arch \
                "$SRC_ROOT"/Userland/Libraries/LibC \
                "$SRC_ROOT"/Userland/Libraries/LibELF/ELFABI.h \
                "$SRC_ROOT"/Userland/Libraries/LibRegex/RegexDefs.h \
                -name '*.h' -print)
            for header in $FILES; do
                target=$(echo "$header" | sed \
                    -e "s|$SRC_ROOT/AK/|AK/|" \
                    -e "s|$SRC_ROOT/Userland/Libraries/LibC||" \
                    -e "s|$SRC_ROOT/Kernel/|Kernel/|" \
                    -e "s|$SRC_ROOT/Userland/Libraries/LibELF/|LibELF/|" \
                    -e "s|$SRC_ROOT/Userland/Libraries/LibRegex/|LibRegex/|")
                buildstep "system_headers" mkdir -p "$(dirname "Root/usr/include/$target")"
                buildstep "system_headers" "$INSTALL" "$header" "Root/usr/include/$target"
            done
            unset SRC_ROOT
        popd

        echo "XXX build jakt support libs"
        buildstep "jakt/support/build/$TOOLCHAIN" "$PREFIX/bin/jakt" cross \
                                                            --only-support-libs \
                                                            --install-root "$SYSROOT/usr/local" \
                                                            --target-triple "$JAKT_TARGET" \
                                                            -C "$TARGET_CXX" \
                                                            -O \
                                                            -J "$NPROC"
    popd
}

for TOOLCHAIN_AND_ARCH in "${VALID_TOOLCHAINS[@]}"; do
    IFS=';' read -r TOOLCHAIN ARCH <<< "$TOOLCHAIN_AND_ARCH"
    buildstep "build/$TOOLCHAIN/$ARCH" build_for "$TOOLCHAIN" "$ARCH"
done

echo "Done creating jakt toolchain for targets " "${VALID_TOOLCHAINS[@]}"
