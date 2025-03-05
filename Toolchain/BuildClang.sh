#!/usr/bin/env bash
set -eo pipefail

# === CONFIGURATION AND SETUP ===

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# shellcheck source=/dev/null
. "${DIR}/../Meta/shell_include.sh"

exit_if_running_as_root "Do not run BuildClang.sh as root, parts of your Toolchain directory will become root-owned"

echo "$DIR"

PREFIX="$DIR/Local/clang/"
BUILD="$DIR/../Build/"
ARCHS="x86_64 aarch64 riscv64"

MD5SUM="md5sum"
REALPATH="realpath"
SED="sed"

SYSTEM_NAME="$(uname -s)"

if [ "$SYSTEM_NAME" = "OpenBSD" ]; then
    MD5SUM="md5 -q"
    REALPATH="readlink -f"
    export CC=egcc
    export CXX=eg++
    export LDFLAGS=-Wl,-z,notext
elif [ "$SYSTEM_NAME" = "FreeBSD" ]; then
    MD5SUM="md5 -q"
elif [ "$SYSTEM_NAME" = "Darwin" ]; then
    MD5SUM="md5 -q"
fi

NPROC=$(get_number_of_processing_units)
[ -z "$MAKEJOBS" ] && MAKEJOBS=${NPROC}

if [ ! -d "$BUILD" ]; then
    mkdir -p "$BUILD"
fi
BUILD=$($REALPATH "$BUILD")

dev=
ci=

while [ "$1" != "" ]; do
    case $1 in
        --dev )           dev=1
                          ;;
        --ci )            ci=1
                          ;;
    esac
    shift
done

if [ "$dev" = "1" ] && [ "$ci" = "1" ]; then
    echo "Please only set one of --dev or --ci."
    exit 1
fi

echo PREFIX is "$PREFIX"

mkdir -p "$DIR/Tarballs"

LLVM_VERSION="20.1.0"
LLVM_MD5SUM="6d38445b43b3d347daee0423e23bbeec"
LLVM_NAME="llvm-project-$LLVM_VERSION.src"
LLVM_PKG="$LLVM_NAME.tar.xz"
LLVM_URL="https://github.com/llvm/llvm-project/releases/download/llvmorg-$LLVM_VERSION/$LLVM_PKG"

buildstep() {
    NAME=$1
    shift
    "$@" 2>&1 | "$SED" $'s|^|\e[34m['"${NAME}"$']\e[39m |'
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

# === DEPENDENCIES ===

buildstep dependencies echo "Checking whether Ninja is available..."
if ! command -v ninja >/dev/null; then
    buildstep dependencies echo "Please make sure to install Ninja."
    exit 1
fi

buildstep dependencies echo "Checking whether CMake is available..."
if ! command -v cmake >/dev/null; then
    buildstep dependencies echo "Please make sure to install CMake."
    exit 1
fi

buildstep dependencies echo "Checking whether 'patch' is available..."
if ! command -v patch >/dev/null; then
    buildstep dependencies echo "Please make sure to install GNU patch."
fi

buildstep dependencies echo "Checking whether your C compiler works..."
if ! ${CC:-cc} -o /dev/null -xc - >/dev/null <<'PROGRAM'
int main() {}
PROGRAM
then
    buildstep dependencies echo "Please make sure to install a working C compiler."
    exit 1
fi

buildstep dependencies echo "Checking whether your C++ compiler works..."
if ! ${CXX:-c++} -o /dev/null -xc - >/dev/null <<'PROGRAM'
int main() {}
PROGRAM
then
    buildstep dependencies echo "Please make sure to install a working C++ compiler."
    exit 1
fi

link_lld=
buildstep dependencies echo "Checking whether the LLD linker is available..."
if ${CXX:-c++} -o /dev/null -fuse-ld=lld -xc - >/dev/null 2>/dev/null << 'PROGRAM'
int main() {}
PROGRAM
then
    link_lld=1
    buildstep dependencies echo "Using LLD for linking LLVM."
else
    buildstep dependencies echo "LLD not found. Using the default linker."
fi

buildstep setup echo "Determining if LLVM should be built with -march=native..."
if [ "$ci" = "1" ]; then
    # The toolchain cache is shared among all runners, which might have different CPUs.
    buildstep setup echo "On a CI runner. Using the default compiler settings."
elif [ -z "${CFLAGS+x}" ] && [ -z "${CXXFLAGS+x}" ]; then
    if ${CXX:-c++} -o /dev/null -march=native -xc - >/dev/null 2>/dev/null << 'PROGRAM'
int main() {}
PROGRAM
    then
        export CFLAGS="-march=native"
        export CXXFLAGS="-march=native"
        buildstep setup echo "Using -march=native for compiling LLVM."
    else
        buildstep setup echo "-march=native is not supported by the compiler. Using the default settings."
    fi
else
    buildstep setup echo "Using user-provided CFLAGS/CXXFLAGS."
fi

# === DOWNLOAD AND PATCH ===

pushd "$DIR/Tarballs"
    md5=""
    if [ -e "$LLVM_PKG" ]; then
        md5="$($MD5SUM ${LLVM_PKG} | cut -f1 -d' ')"
        echo "llvm md5='$md5'"
    fi

    if [ "$md5" != "$LLVM_MD5SUM" ] ; then
        rm -f "$LLVM_PKG"
        curl -LO "$LLVM_URL"
    else
        echo "Skipped downloading LLVM"
    fi

    patch_md5="$($MD5SUM "$DIR"/Patches/llvm/*.patch)"

    if [ ! -d "$LLVM_NAME" ] || [ "$(cat $LLVM_NAME/.patch.applied)" != "$patch_md5" ]; then
        if [ -d "$LLVM_NAME" ]; then
            # Drop the previously patched extracted dir
            rm -rf "${LLVM_NAME}"
        fi

        rm -rf "$DIR/Build/clang"

        echo "Extracting LLVM..."
        tar -xJf "$LLVM_PKG"

        pushd "$LLVM_NAME"
            if [ "$dev" = "1" ]; then
                git init > /dev/null
                git add . > /dev/null
                git commit -am "BASE" > /dev/null
                git am --keep-non-patch "$DIR"/Patches/llvm/*.patch > /dev/null
            else
                for patch in "$DIR"/Patches/llvm/*.patch; do
                    patch -p1 < "$patch" > /dev/null
                done
            fi
            echo "$patch_md5" > .patch.applied
        popd
    else
        echo "Using existing LLVM source directory"
    fi
popd

# === COPY HEADERS ===

SRC_ROOT=$($REALPATH "$DIR"/..)
FILES=$(find \
    "$SRC_ROOT"/Kernel/API \
    "$SRC_ROOT"/Kernel/Arch \
    "$SRC_ROOT"/Userland/Libraries/LibC \
    "$SRC_ROOT"/Userland/Libraries/LibELF/ELFABI.h \
    "$SRC_ROOT"/Userland/Libraries/LibRegex/RegexDefs.h \
    -name '*.h' -print)
for arch in $ARCHS; do
    mkdir -p "$BUILD/${arch}clang"
    pushd "$BUILD/${arch}clang"
        for header in $FILES; do
            target=$(echo "$header" | "$SED" \
                -e "s|$SRC_ROOT/Kernel/|Kernel/|" \
                -e "s|$SRC_ROOT/Userland/Libraries/LibC||" \
                -e "s|$SRC_ROOT/Userland/Libraries/LibELF/|LibELF/|" \
                -e "s|$SRC_ROOT/Userland/Libraries/LibRegex/|LibRegex/|")
            mkdir -p "$(dirname "Root/usr/include/$target")"
            buildstep "system_headers" cp "$header" "Root/usr/include/$target"
        done
    popd
done
unset SRC_ROOT

# === COMPILE AND INSTALL ===

rm -rf "$PREFIX"
mkdir -p "$PREFIX"

mkdir -p "$DIR/Build/clang"

pushd "$DIR/Build/clang"
    mkdir -p llvm
    pushd llvm
        buildstep "llvm/configure" cmake "$DIR/Tarballs/$LLVM_NAME/llvm" \
            -G Ninja \
            -DSERENITY_x86_64-pc-serenity_SYSROOT="$BUILD/x86_64clang/Root" \
            -DSERENITY_aarch64-pc-serenity_SYSROOT="$BUILD/aarch64clang/Root" \
            -DSERENITY_riscv64-pc-serenity_SYSROOT="$BUILD/riscv64clang/Root" \
            -DSERENITY_x86_64-pc-serenity_STUBS="$DIR/Stubs/x86_64" \
            -DSERENITY_aarch64-pc-serenity_STUBS="$DIR/Stubs/aarch64" \
            -DSERENITY_riscv64-pc-serenity_STUBS="$DIR/Stubs/riscv64" \
            -DCMAKE_INSTALL_PREFIX="$PREFIX" \
            -C "$DIR/CMake/LLVMConfig.cmake" \
            ${link_lld:+"-DLLVM_ENABLE_LLD=ON"} \
            ${dev:+"-DLLVM_CCACHE_BUILD=ON"} \
            ${ci:+"-DLLVM_CCACHE_BUILD=ON"} \
            ${ci:+"-DLLVM_CCACHE_DIR=$LLVM_CCACHE_DIR"}

        buildstep_ninja "llvm/build" ninja -j "$MAKEJOBS"
        buildstep_ninja "llvm/install" ninja install/strip
    popd
popd

pushd "$DIR/Local/clang/bin/"
    ln -s ../../mold/bin/mold ld.mold

    for arch in $ARCHS; do
        ln -s clang "$arch"-pc-serenity-clang
        ln -s clang++ "$arch"-pc-serenity-clang++
        ln -s llvm-nm "$arch"-pc-serenity-nm
        echo "--sysroot=$BUILD/${arch}clang/Root" > "$arch"-pc-serenity.cfg
    done
popd
