#!/usr/bin/env bash
set -eo pipefail

# === CONFIGURATION AND SETUP ===

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "$DIR"

PREFIX="$DIR/Local/clang/"
BUILD="$DIR/../Build/"
USERLAND_ARCHS="i686 x86_64"
ARCHS="$USERLAND_ARCHS aarch64"

MD5SUM="md5sum"
REALPATH="realpath"
MAKE="make"
NPROC="nproc"

SYSTEM_NAME="$(uname -s)"

if [ "$SYSTEM_NAME" = "OpenBSD" ]; then
    MD5SUM="md5 -q"
    REALPATH="readlink -f"
    MAKE="gmake"
    NPROC="sysctl -n hw.ncpuonline"
    export CC=egcc
    export CXX=eg++
    export LDFLAGS=-Wl,-z,notext
elif [ "$SYSTEM_NAME" = "FreeBSD" ]; then
    MD5SUM="md5 -q"
    MAKE="gmake"
    NPROC="sysctl -n hw.ncpu"
fi

if [ -z "$MAKEJOBS" ]; then
    MAKEJOBS=$($NPROC)
fi


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

LLVM_VERSION="13.0.0"
LLVM_MD5SUM="bfc5191cbe87954952d25c6884596ccb"
LLVM_NAME="llvm-project-$LLVM_VERSION.src"
LLVM_PKG="$LLVM_NAME.tar.xz"
LLVM_URL="https://github.com/llvm/llvm-project/releases/download/llvmorg-$LLVM_VERSION/$LLVM_PKG"

# We need GNU binutils because we use a feature that llvm-objdump doesn't support yet.
BINUTILS_VERSION="2.37"
BINUTILS_MD5SUM="1e55743d73c100b7a0d67ffb32398cdb"
BINUTILS_NAME="binutils-$BINUTILS_VERSION"
BINUTILS_PKG="${BINUTILS_NAME}.tar.gz"
BINUTILS_BASE_URL="https://ftp.gnu.org/gnu/binutils"

buildstep() {
    NAME=$1
    shift
    "$@" 2>&1 | sed $'s|^|\e[34m['"${NAME}"$']\e[39m |'
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

buildstep dependencies echo "Checking whether 'make' is available..."
if ! command -v ${MAKE:-make} >/dev/null; then
    buildstep dependencies echo "Please make sure to install GNU Make (for the '${MAKE:-make}' tool)."
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

# === CHECK CACHE AND REUSE ===

pushd "$DIR"
    if [ "$TRY_USE_LOCAL_TOOLCHAIN" = "y" ]; then
        mkdir -p Cache
        echo "Cache (before):"
        ls -l Cache
        CACHED_TOOLCHAIN_ARCHIVE="Cache/ToolchainBinariesGithubActions.tar.gz"
        if [ -r "${CACHED_TOOLCHAIN_ARCHIVE}" ] ; then
            echo "Cache at ${CACHED_TOOLCHAIN_ARCHIVE} exists!"
            echo "Extracting toolchain from cache:"
            if tar xzf "${CACHED_TOOLCHAIN_ARCHIVE}" ; then
                 echo "Done 'building' the toolchain."
                 echo "Cache unchanged."
                 exit 0
            else
                echo
                echo
                echo
                echo "Could not extract cached toolchain archive."
                echo "This means the cache is broken and *should be removed*!"
                echo "As Github Actions cannot update a cache, this will unnecessarily"
                echo "slow down all future builds for this hash, until someone"
                echo "resets the cache."
                echo
                echo
                echo
                rm -f "${CACHED_TOOLCHAIN_ARCHIVE}"
            fi
        else
            echo "Cache at ${CACHED_TOOLCHAIN_ARCHIVE} does not exist."
            echo "Will rebuild toolchain from scratch, and save the result."
        fi
    fi
    echo "::group::Actually building Toolchain"
popd

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

    if [ -d "$LLVM_NAME" ]; then
        # Drop the previously patched extracted dir
        rm -rf "${LLVM_NAME}"
        # Also drop the build dir
        rm -rf "$DIR/Build/clang"
    fi
    echo "Extracting LLVM..."
    tar -xJf "$LLVM_PKG"

    pushd "$LLVM_NAME"
        if [ "$dev" = "1" ]; then
            git init > /dev/null
            git add . > /dev/null
            git commit -am "BASE" > /dev/null
            git apply "$DIR"/Patches/llvm.patch > /dev/null
        else
            patch -p1 < "$DIR/Patches/llvm.patch" > /dev/null
        fi
        $MD5SUM "$DIR/Patches/llvm.patch" > .patch.applied
    popd

    md5=""
    if [ -e "$BINUTILS_PKG" ]; then
        md5="$($MD5SUM $BINUTILS_PKG | cut -f1 -d' ')"
        echo "bu md5='$md5'"
    fi

    if [ "$md5" != "$BINUTILS_MD5SUM" ]; then
        rm -f "$BINUTILS_PKG"
        curl -LO "$BINUTILS_BASE_URL/$BINUTILS_PKG"
    else
        echo "Skipped downloading GNU binutils"
    fi

    if [ -d "$BINUTILS_NAME" ]; then
        rm -rf "$BINUTILS_NAME"
        rm -rf "$DIR/Build/clang/binutils"
    fi
    echo "Extracting GNU binutils"


    tar -xzf "$BINUTILS_PKG"
    pushd "$BINUTILS_NAME"
        if [ "$dev" = "1" ]; then
            git init > /dev/null
            git add . > /dev/null
            git commit -am "BASE" > /dev/null
            git apply "$DIR"/Patches/binutils.patch > /dev/null
        else
            patch -p1 < "$DIR/Patches/binutils.patch" > /dev/null
        fi
         $MD5SUM "$DIR/Patches/binutils.patch" > .patch.applied
    popd
popd

# === COPY HEADERS ===

SRC_ROOT=$($REALPATH "$DIR"/..)
FILES=$(find "$SRC_ROOT"/Kernel/API "$SRC_ROOT"/Userland/Libraries/LibC "$SRC_ROOT"/Userland/Libraries/LibM "$SRC_ROOT"/Userland/Libraries/LibPthread "$SRC_ROOT"/Userland/Libraries/LibDl -name '*.h' -print)

for arch in $ARCHS; do
    mkdir -p "$BUILD/${arch}clang"
    pushd "$BUILD/${arch}clang"
        mkdir -p Root/usr/include/
        for header in $FILES; do
            target=$(echo "$header" | sed -e "s@$SRC_ROOT/Userland/Libraries/LibC@@" -e "s@$SRC_ROOT/Userland/Libraries/LibM@@" -e "s@$SRC_ROOT/Userland/Libraries/LibPthread@@" -e "s@$SRC_ROOT/Userland/Libraries/LibDl@@" -e "s@$SRC_ROOT/Kernel/@Kernel/@")
            buildstep "system_headers" install -D "$header" "Root/usr/include/$target"
        done
    popd
done
unset SRC_ROOT

# === COPY LIBRARY STUBS ===

for arch in $USERLAND_ARCHS; do
    pushd "$BUILD/${arch}clang"
        mkdir -p Root/usr/lib/
        for lib in "$DIR/Stubs/${arch}clang/"*".so"; do
            lib_name=$(basename "$lib")
            [ ! -f "Root/usr/lib/${lib_name}.so" ] && cp "$lib" "Root/usr/lib/${lib_name}"
        done
    popd
done

# === COMPILE AND INSTALL ===

rm -rf "$PREFIX"
mkdir -p "$PREFIX"

mkdir -p "$DIR/Build/clang"

pushd "$DIR/Build/clang"
    mkdir -p llvm
    pushd llvm
        buildstep "llvm/configure" cmake "$DIR/Tarballs/$LLVM_NAME/llvm" \
            -G Ninja \
            -DSERENITY_i686-pc-serenity_SYSROOT="$BUILD/i686clang/Root" \
            -DSERENITY_x86_64-pc-serenity_SYSROOT="$BUILD/x86_64clang/Root" \
            -DSERENITY_aarch64-pc-serenity_SYSROOT="$BUILD/aarch64clang/Root" \
            -DCMAKE_INSTALL_PREFIX="$PREFIX" \
            -C "$DIR/CMake/LLVMConfig.cmake" \
            ${dev:+"-DLLVM_CCACHE_BUILD=ON"} \
            ${ci:+"-DLLVM_CCACHE_BUILD=ON"} \
            ${ci:+"-DLLVM_CCACHE_DIR=$LLVM_CCACHE_DIR"} \
            ${ci:+"-DLLVM_CCACHE_MAXSIZE=$LLVM_CCACHE_MAXSIZE"}

        buildstep_ninja "llvm/build" ninja -j "$MAKEJOBS"
        buildstep "llvm/install" ninja install/strip
    popd

    mkdir -p binutils
    pushd binutils
        buildstep "binutils/configure" "$DIR/Tarballs/$BINUTILS_NAME/configure" --prefix="$PREFIX" \
            --enable-targets="$(echo "$ARCHS" | sed -E "s@(\S)(\s|$)@\1-pc-serenity,@g")" \
            --program-prefix="gnu-" \
            --disable-nls \
            --disable-gas \
            --disable-gold \
            --disable-ld \
            --disable-gprof \
            --enable-shared
        buildstep "binutils/build" "$MAKE" -j "$MAKEJOBS"
        buildstep "binutils/install" "$MAKE" install
    popd

    for arch in $ARCHS; do
        mkdir -p runtimes/"$arch"
        pushd runtimes/"$arch"
            buildstep "runtimes/$arch/configure" cmake "$DIR/Tarballs/$LLVM_NAME/runtimes" \
                -G Ninja \
                -DSERENITY_TOOLCHAIN_ARCH="$arch" \
                -DSERENITY_TOOLCHAIN_ROOT="$PREFIX" \
                -DSERENITY_BUILD_DIR="$BUILD/${arch}clang/" \
                -DCMAKE_INSTALL_PREFIX="$PREFIX" \
                -C "$DIR/CMake/LLVMRuntimesConfig.cmake"

            buildstep "runtimes/$arch/build" ninja -j "$MAKEJOBS"
            buildstep "runtimes/$arch/install" ninja install
        popd
    done
popd

# === SAVE TO CACHE ===

pushd "$DIR"
    if [ "$TRY_USE_LOCAL_TOOLCHAIN" = "y" ]; then
        echo "::endgroup::"
        echo "Building cache tar:"

        echo "Building cache tar:"

        rm -f "${CACHED_TOOLCHAIN_ARCHIVE}"  # Just in case

        tar czf "${CACHED_TOOLCHAIN_ARCHIVE}" Local/
        echo "Cache (after):"
        ls -l Cache
    fi
popd
