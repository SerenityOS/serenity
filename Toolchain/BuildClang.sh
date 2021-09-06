#!/usr/bin/env bash
set -eo pipefail

# === CONFIGURATION AND SETUP ===

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "$DIR"

ARCH=${ARCH:-"i686"}
LLVM_ARCH=
[ "$ARCH" = "i686" ] && LLVM_ARCH="i386" || LLVM_ARCH="$ARCH"
LLVM_TARGET="$LLVM_ARCH-pc-serenity"
PREFIX="$DIR/Local/clang/$ARCH"
BUILD="$DIR/../Build/clang/$ARCH"
SYSROOT="$BUILD/Root"

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
while [ "$1" != "" ]; do
    case $1 in
        --dev )           dev=1
                          ;;
    esac
    shift
done

echo PREFIX is "$PREFIX"
echo SYSROOT is "$SYSROOT"

mkdir -p "$DIR/Tarballs"

LLVM_VERSION="12.0.1"
LLVM_MD5SUM="c28061313a4f1b7d74cd491a19f569b4"
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
    "$@" 2>&1 | sed $'s|^|\x1b[34m['"${NAME}"$']\x1b[39m |'
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
        rm -rf "$DIR/Build/clang/$ARCH"
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
        rm -rf "$DIR/Build/clang/$ARCH/binutils"
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

# === COPY SERENITYOS HEADERS ===

mkdir -p "$BUILD"
pushd "$BUILD"
    mkdir -p Root/usr/include/
    SRC_ROOT=$($REALPATH "$DIR"/..)
    FILES=$(find "$SRC_ROOT"/Kernel/API "$SRC_ROOT"/Userland/Libraries/LibC "$SRC_ROOT"/Userland/Libraries/LibM "$SRC_ROOT"/Userland/Libraries/LibPthread "$SRC_ROOT"/Userland/Libraries/LibDl -name '*.h' -print)
    for header in $FILES; do
        target=$(echo "$header" | sed -e "s@$SRC_ROOT/Userland/Libraries/LibC@@" -e "s@$SRC_ROOT/Userland/Libraries/LibM@@" -e "s@$SRC_ROOT/Userland/Libraries/LibPthread@@" -e "s@$SRC_ROOT/Userland/Libraries/LibDl@@" -e "s@$SRC_ROOT/Kernel/@Kernel/@")
        buildstep "system_headers" install -D "$header" "Root/usr/include/$target"
    done
    unset SRC_ROOT
popd

# === COMPILE AND INSTALL ===

rm -rf "$PREFIX"
mkdir -p "$PREFIX"

mkdir -p "$DIR/Build/clang/$ARCH"

pushd "$DIR/Build/clang/$ARCH"

    mkdir -p llvm
    pushd llvm
        buildstep "llvm+clang/configure" cmake "$DIR/Tarballs/llvm-project-$LLVM_VERSION.src/llvm" \
            -G Ninja \
            -DCMAKE_BUILD_TYPE="MinSizeRel" \
            -DCMAKE_INSTALL_PREFIX="$PREFIX" \
            -DLLVM_DEFAULT_TARGET_TRIPLE="$LLVM_TARGET" \
            '-DLLVM_TARGETS_TO_BUILD=X86;AArch64' \
            -DLLVM_ENABLE_BINDINGS=OFF \
            -DLLVM_ENABLE_PER_TARGET_RUNTIME_DIR=OFF \
            -DLLVM_ENABLE_PROJECTS="clang;lld" \
            -DLLVM_INCLUDE_BENCHMARKS=OFF \
            -DLLVM_INCLUDE_TESTS=OFF \
            -DLLVM_LLVM_BUILD_LLVM_DYLIB=ON \
            -DLLVM_LINK_LLVM_DYLIB=ON \
            -DLLVM_INSTALL_UTILS=OFF \
            ${dev:+"-DLLVM_CCACHE_BUILD=ON"} || exit 1

        buildstep "llvm+clang/build" ninja -j "$MAKEJOBS" || exit 1
        buildstep "llvm+clang/install" ninja install || exit 1
    popd

    LLVM_COMPILER_FLAGS="-target $LLVM_TARGET --sysroot $SYSROOT -ftls-model=initial-exec -ffreestanding -nostdlib -nostdlib++"

    mkdir -p "compiler-rt"
    pushd compiler-rt
        buildstep "compiler-rt/configure" cmake "$DIR/Tarballs/llvm-project-$LLVM_VERSION.src/compiler-rt" \
            -GNinja \
            -DCMAKE_SYSROOT="$SYSROOT" \
            -DCMAKE_INSTALL_PREFIX="$PREFIX/lib/clang/${LLVM_VERSION}" \
            -DCMAKE_BUILD_TYPE="Release" \
            -DCMAKE_AR="$PREFIX/bin/llvm-ar" \
            -DCMAKE_C_COMPILER="$PREFIX/bin/clang" \
            -DCMAKE_C_COMPILER_TARGET="$LLVM_TARGET" \
            -DCMAKE_C_FLAGS="$LLVM_COMPILER_FLAGS" \
            -DCMAKE_CXX_COMPILER="$PREFIX/bin/clang++" \
            -DCMAKE_CXX_FLAGS="$LLVM_COMPILER_FLAGS" \
            -DCMAKE_CXX_COMPILER_TARGET="$LLVM_TARGET" \
            -DCMAKE_ASM_COMPILER="$PREFIX/bin/clang" \
            -DCMAKE_ASM_COMPILER_TARGET="$LLVM_TARGET" \
            -DCMAKE_ASM_FLAGS="$LLVM_COMPILER_FLAGS" \
            -DLLVM_CONFIG_PATH="$PREFIX/bin/llvm-config" \
            -DCOMPILER_RT_EXCLUDE_ATOMIC_BUILTIN=OFF \
            -DCOMPILER_RT_OS_DIR="serenity" \
            -DCOMPILER_RT_DEFAULT_TARGET_ONLY=ON \
            -DCOMPILER_RT_BUILD_BUILTINS=ON \
            -DCOMPILER_RT_BUILD_CRT=ON \
            -DCOMPILER_RT_BUILD_LIBFUZZER=OFF \
            -DCOMPILER_RT_BUILD_MEMPROF=OFF \
            -DCOMPILER_RT_BUILD_PROFILE=OFF \
            -DCOMPILER_RT_BUILD_SANITIZERS=OFF \
            -DCOMPILER_RT_BUILD_XRAY=OFF || exit 1

        buildstep "compiler-rt/build" ninja -j "$MAKEJOBS" || exit 1
        buildstep "compiler-rt/install" ninja install || exit 1
    popd

    mkdir -p libunwind
    pushd libunwind
        buildstep "libunwind/configure" cmake "$DIR/Tarballs/llvm-project-$LLVM_VERSION.src/libunwind" \
            -GNinja \
            -DCMAKE_BUILD_TYPE="Release" \
            -DCMAKE_SYSROOT="$SYSROOT" \
            -DCMAKE_C_COMPILER="$PREFIX/bin/clang" \
            -DCMAKE_C_COMPILER_TARGET="$LLVM_TARGET" \
            -DCMAKE_C_FLAGS="$LLVM_COMPILER_FLAGS" \
            -DCMAKE_CXX_COMPILER="$PREFIX/bin/clang++" \
            -DCMAKE_CXX_COMPILER_TARGET="$LLVM_TARGET" \
            -DCMAKE_CXX_FLAGS="$LLVM_COMPILER_FLAGS" \
            -DCMAKE_INSTALL_PREFIX="$PREFIX" \
            -DLLVM_CONFIG_PATH="$PREFIX/bin/llvm-config" \
            -DLIBUNWIND_TARGET_TRIPLE="$LLVM_TARGET" \
            -DLIBUNWIND_SYSROOT="$SYSROOT" || exit 1

        buildstep "libunwind/build" ninja -j "$MAKEJOBS" || exit 1
        buildstep "libunwind/install" ninja install || exit 1
    popd

    mkdir -p libcxxabi
    pushd libcxxabi
         buildstep "libcxxabi/configure" cmake "$DIR/Tarballs/llvm-project-$LLVM_VERSION.src/libcxxabi" \
            -GNinja \
            -DCMAKE_BUILD_TYPE="Release" \
            -DCMAKE_SYSROOT="$SYSROOT" \
            -DCMAKE_C_COMPILER="$PREFIX/bin/clang" \
            -DCMAKE_C_COMPILER_TARGET="$LLVM_TARGET" \
            -DCMAKE_C_FLAGS="$LLVM_COMPILER_FLAGS" \
            -DCMAKE_CXX_COMPILER="$PREFIX/bin/clang++" \
            -DCMAKE_CXX_COMPILER_TARGET="$LLVM_TARGET" \
            -DCMAKE_CXX_FLAGS="$LLVM_COMPILER_FLAGS" \
            -DCMAKE_INSTALL_PREFIX="$PREFIX" \
            -DLLVM_CONFIG_PATH="$PREFIX/bin/llvm-config" \
            -DLIBCXXABI_STANDALONE_BUILD=ON \
            -DLIBCXXABI_ENABLE_EXCEPTIONS=ON \
            -DLIBCXXABI_STANDALONE_BUILD=ON \
            -DLIBCXXABI_ENABLE_ASSERTIONS=OFF \
            -DLIBCXXABI_BAREMETAL=ON || exit 1

        buildstep "libcxxabi/build" ninja -j "$MAKEJOBS" || exit 1
        buildstep "libcxxabi/install" ninja install || exit 1
    popd

    mkdir -p libcxx
    pushd libcxx
        buildstep "libcxx/configure" cmake "$DIR/Tarballs/llvm-project-$LLVM_VERSION.src/libcxx" \
            -G Ninja \
            -DCMAKE_BUILD_TYPE="Release" \
            -DCMAKE_BINARY_DIR="$PREFIX/usr" \
            -DCMAKE_SYSROOT="$SYSROOT" \
            -DCMAKE_C_COMPILER="$PREFIX/bin/clang" \
            -DCMAKE_C_FLAGS="$LLVM_COMPILER_FLAGS" \
            -DCMAKE_CXX_COMPILER="$PREFIX/bin/clang++" \
            -DCMAKE_CXX_FLAGS="$LLVM_COMPILER_FLAGS" \
            -DCMAKE_INSTALL_PREFIX="$PREFIX" \
            -DLLVM_CONFIG_PATH="$PREFIX/bin/llvm-config" \
            -DLLVM_BINARY_DIR="$PREFIX/usr" \
            -DLIBCXX_INSTALL_LIBRARY=ON \
            -DLIBCXX_ENABLE_LOCALIZATION=OFF \
            -DLIBCXX_ENABLE_FILESYSTEM=OFF \
            -DLIBCXX_INSTALL_HEADERS=ON \
            -DLIBCXX_ENABLE_SHARED=ON \
            -DLIBCXX_ENABLE_LOCALIZATION=OFF \
            -DLIBCXX_ENABLE_STATIC=ON \
            -DLIBCXX_CXX_ABI="libcxxabi" \
            -DLIBCXX_INCLUDE_BENCHMARKS=OFF || exit 1

        buildstep "libcxx/build" ninja -j "$MAKEJOBS" || exit 1
        buildstep "libcxx/install" ninja install || exit 1
        buildstep "libcxx/install-headers" ninja install-cxx-headers || exit 1
    popd

    mkdir -p binutils
    pushd binutils
        buildstep "binutils/configure" "$DIR/Tarballs/$BINUTILS_NAME/configure" --prefix="$PREFIX/binutils" \
            --target="$ARCH-pc-serenity" \
            --disable-nls \
            --disable-gas \
            --disable-gold \
            --disable-ld \
            --disable-gprof \
            --enable-shared || exit 1
        buildstep "binutils/build" "$MAKE" -j "$MAKEJOBS" || exit 1
        buildstep "binutils/install" "$MAKE" install || exit 1
    popd
popd

# === REMOVE UNNECESSARY BUILD ARTIFACTS ===
rm -r "$PREFIX"/lib/libclang*.a "$PREFIX"/lib/libLLVM*.a "$PREFIX"/lib/liblld*.a

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
