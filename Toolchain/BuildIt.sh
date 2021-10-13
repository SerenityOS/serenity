#!/usr/bin/env bash
set -eo pipefail
# This file will need to be run in bash, for now.

# Helper function to prefix our script output
buildstep() {
    NAME=$1
    shift
    "$@" 2>&1 | sed $'s|^|\x1b[34m['"${NAME}"$']\x1b[39m |'
}

# === CONFIGURATION AND SETUP ===

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

buildstep BuildIt_start printf "\n\n\t\tRunning %s from %s/ ...\n\n\n" "${BASH_SOURCE[0]}" "$DIR"

ARCH=${ARCH:-"i686"}
TARGET="$ARCH-pc-serenity"
PREFIX="$DIR/Local/$ARCH"
BUILD="$DIR/../Build/$ARCH"
SYSROOT="$BUILD/Root"

MAKE="make"
SHA256SUM="sha256sum"
NPROC="nproc"
REALPATH="realpath"

if command -v ginstall &>/dev/null; then
    INSTALL=ginstall
else
    INSTALL=install
fi

SYSTEM_NAME="$(uname -s)"

# We *most definitely* don't need debug symbols in the linker/compiler.
# This cuts the uncompressed size from 1.2 GiB per Toolchain down to about 120 MiB.
# Hence, this might actually cause marginal speedups, although the point is to not waste space as blatantly.
export CFLAGS="-g0 -O2 -mtune=native"
export CXXFLAGS="-g0 -O2 -mtune=native"

if [ "$SYSTEM_NAME" = "OpenBSD" ]; then
    MAKE=gmake
    SHA256SUM="sha256 -q"
    NPROC="sysctl -n hw.ncpuonline"
    REALPATH="readlink -f"
    export CC=egcc
    export CXX=eg++
    export with_gmp=/usr/local
    export LDFLAGS=-Wl,-z,notext
elif [ "$SYSTEM_NAME" = "FreeBSD" ]; then
    MAKE=gmake
    SHA256SUM="sha256 -q"
    NPROC="sysctl -n hw.ncpu"
    export with_gmp=/usr/local
    export with_mpfr=/usr/local
fi

# On at least OpenBSD, the path must exist to call realpath(3) on it
if [ ! -d "$BUILD" ]; then
    mkdir -p "$BUILD"
fi
BUILD=$($REALPATH "$BUILD")

git_patch=
while [ "$1" != "" ]; do
    case $1 in
        --dev )           git_patch=1
                          ;;
    esac
    shift
done

buildstep BuildIt_start printf "PREFIX is '%s'\n" "$PREFIX"
buildstep BuildIt_start printf "SYSROOT is '%s'\n" "$SYSROOT"

mkdir -p "$DIR/Tarballs"

# Note: The version number and hash in BuildClang.sh needs to be kept in sync with this.
BINUTILS_VERSION="2.37"
BINUTILS_SHA256SUM="820d9724f020a3e69cb337893a0b63c2db161dadcb0e06fc11dc29eb1e84a32c"
BINUTILS_NAME="binutils-$BINUTILS_VERSION"
BINUTILS_PKG="${BINUTILS_NAME}.tar.xz"
BINUTILS_BASE_URL="https://ftp.gnu.org/gnu/binutils" # option if that is slow: https://fossies.org/linux/misc/

GDB_VERSION="10.2"
GDB_SHA256SUM="aaa1223d534c9b700a8bec952d9748ee1977513f178727e1bee520ee000b4f29"
GDB_NAME="gdb-$GDB_VERSION"
GDB_PKG="${GDB_NAME}.tar.xz"
GDB_BASE_URL="https://ftp.gnu.org/gnu/gdb" # option if that is slow: https://fossies.org/linux/misc/legacy/

# Note: If you bump the gcc version, you also have to update the matching
#       GCC_VERSION variable in the project's root CMakeLists.txt
GCC_VERSION="11.2.0"
GCC_SHA256SUM="d08edc536b54c372a1010ff6619dd274c0f1603aa49212ba20f7aa2cda36fa8b"
GCC_NAME="gcc-$GCC_VERSION"
GCC_PKG="${GCC_NAME}.tar.xz"
GCC_BASE_URL="https://ftp.gnu.org/gnu/gcc" # option if that is slow: https://fossies.org/linux/misc/


# === DEPENDENCIES ===
buildstep dependencies printf "Checking whether 'make' is available...\n"
if ! command -v ${MAKE:-make} >/dev/null; then
    buildstep dependencies printf "Please make sure to install GNU Make (for the '%s' tool).\n" "${MAKE:-make}"
    exit 1
fi

buildstep dependencies printf "Checking whether 'patch' is available...\n"
if ! command -v patch >/dev/null; then
    buildstep dependencies printf "Please make sure to install GNU patch (for the 'patch' tool).\n"
    exit 1
fi

buildstep dependencies printf "Checking whether your C compiler works...\n"
if ! ${CC:-cc} -o /dev/null -xc - >/dev/null <<'PROGRAM'
int main() {}
PROGRAM
then
    buildstep dependencies printf "Please make sure to install a working C compiler.\n"
    exit 1
fi

if [ "$SYSTEM_NAME" != "Darwin" ]; then
    for lib in gmp mpc mpfr; do
        buildstep dependencies printf "Checking whether the %s library and headers are available...\n" "$lib"
        if ! ${CC:-cc} -I /usr/local/include -L /usr/local/lib -l$lib -o /dev/null -xc - >/dev/null <<PROGRAM
#include <$lib.h>
int main() {}
PROGRAM
        then
            buildstep dependencies printf "Please make sure to install the %s library and headers.\n" "$lib"
            exit 1
        fi
    done
fi

# === CHECK CACHE AND REUSE ===

pushd "$DIR"
    if [ "${TRY_USE_LOCAL_TOOLCHAIN}" = "y" ] ; then
        # The actual logic had to be moved to .github/workflows/cmake.yml.
        # Github Actions guarantees that Toolchain/Cache/ is empty on a cache
        # miss, and non-empty on a cache hit.
        # The following logic is correct *only* because of that.

        mkdir -p Cache
        buildstep cachecheck printf "Cache (before):\n"
        ls -l Cache
        CACHED_TOOLCHAIN_ARCHIVE="Cache/ToolchainBinariesGithubActions.tar.gz"
        if [ -r "${CACHED_TOOLCHAIN_ARCHIVE}" ] ; then
            buildstep cacheuse printf "Cache at %s exists!\n" "${CACHED_TOOLCHAIN_ARCHIVE}"
            buildstep cacheuse printf "Extracting toolchain from cache:\n"
            if tar xzf "${CACHED_TOOLCHAIN_ARCHIVE}" ; then
                buildstep cacheuse printf "Done 'building' the toolchain.\n"
                buildstep cacheuse printf "Cache unchanged.\n"
                exit 0
            else
                printf "\n\n\n\n"
                printf "Could not extract cached toolchain archive.\n"
                printf "This means the cache is broken and *should be removed*!\n"
                printf "As Github Actions cannot update a cache, this will unnecessarily\n"
                printf "slow down all future builds for this hash, until someone\n"
                printf "resets the cache.\n"
                printf "\n\n\n"
                rm -f "${CACHED_TOOLCHAIN_ARCHIVE}"
            fi
        else
            buildstep cachemissing printf "Cache at %s does not exist.\n" "${CACHED_TOOLCHAIN_ARCHIVE}"
            buildstep cachemissing printf "Will rebuild toolchain from scratch, and save the result.\n"
        fi
        buildstep cachemissing printf "::group::Actually building Toolchain\n"
    fi
popd


# === DOWNLOAD AND PATCH ===

buildstep toolchain_dl printf "\tChecking if we must download toolchain tarballs (gdb, binutils, gcc) ...\n"
buildstep toolchain_dl printf "\tSometimes the GNU servers we pull these from are particularly slow.\n"
buildstep toolchain_dl printf "\tIf this is taking a very long time, you may want to manually download\n"
buildstep toolchain_dl printf "\tthe right tarballs here from another source.\n"
buildstep toolchain_dl printf "\t(this script is: %s/%s )\n" "$DIR" "${BASH_SOURCE[0]}"
pushd "$DIR/Tarballs"
    # Build aarch64-gdb for cross-debugging support on x86 systems
    if [ "$ARCH" = "aarch64" ]; then
        SHA256=""
        if [ -e "$GDB_PKG" ]; then
            SHA256="$($SHA256SUM $GDB_PKG | cut -f1 -d' ')"
            buildstep toolchain_dl printf "bu SHA256='%s'\n" "$SHA256"
        fi
        if [ "$SHA256" != ${GDB_SHA256SUM} ] ; then
            rm -f $GDB_PKG
            curl -LO "$GDB_BASE_URL/$GDB_PKG"
        else
            buildstep toolchain_dl printf "Skipped downloading gdb, it is already present and intact.\n"
        fi
    fi

    SHA256=""
    if [ -e "$BINUTILS_PKG" ]; then
        SHA256="$($SHA256SUM $BINUTILS_PKG | cut -f1 -d' ')"
        buildstep toolchain_dl printf "bu SHA256='%s'\n" "$SHA256"
    fi
    if [ "$SHA256" != ${BINUTILS_SHA256SUM} ] ; then
        rm -f $BINUTILS_PKG
        curl -LO "$BINUTILS_BASE_URL/$BINUTILS_PKG"
    else
        buildstep toolchain_dl printf "Skipped downloading binutils, it is already present and intact.\n"
    fi

    SHA256=""
    if [ -e "$GCC_PKG" ]; then
        SHA256="$($SHA256SUM ${GCC_PKG} | cut -f1 -d' ')"
        buildstep toolchain_dl printf "gc SHA256='%s'\n" "$SHA256"
    fi
    if [ "$SHA256" != ${GCC_SHA256SUM} ] ; then
        rm -f $GCC_PKG
        curl -LO "$GCC_BASE_URL/$GCC_NAME/$GCC_PKG"
    else
        buildstep toolchain_dl printf "Skipped downloading gcc, it is already present and intact.\n"
    fi

    if [ "$ARCH" = "aarch64" ]; then
        if [ -d ${GDB_NAME} ]; then
            rm -rf "${GDB_NAME}"
            rm -rf "$DIR/Build/$ARCH/$GDB_NAME"
        fi
        buildstep toolchain_dl printf "\n\tExtracting GDB...\n\n"
        tar -xJf ${GDB_PKG}

        pushd ${GDB_NAME}
            if [ "$git_patch" = "1" ]; then
                git init > /dev/null
                git add . > /dev/null
                git commit -am "BASE" > /dev/null
                git apply "$DIR"/Patches/gdb.patch > /dev/null
            else
                patch -p1 < "$DIR"/Patches/gdb.patch > /dev/null
            fi
            $SHA256SUM "$DIR"/Patches/gdb.patch > .patch.applied
        popd
    fi

    if [ -d ${BINUTILS_NAME} ]; then
        rm -rf "${BINUTILS_NAME}"
        rm -rf "$DIR/Build/$ARCH/$BINUTILS_NAME"
    fi
    buildstep toolchain_dl printf "\n\tExtracting binutils...\n\n"
    tar -xJf ${BINUTILS_PKG}

    pushd ${BINUTILS_NAME}
        if [ "$git_patch" = "1" ]; then
            git init > /dev/null
            git add . > /dev/null
            git commit -am "BASE" > /dev/null
            git apply "$DIR"/Patches/binutils.patch > /dev/null
        else
            patch -p1 < "$DIR"/Patches/binutils.patch > /dev/null
        fi
        $SHA256SUM "$DIR"/Patches/binutils.patch > .patch.applied
    popd

    if [ -d ${GCC_NAME} ]; then
        # Drop the previously patched extracted dir
        rm -rf "${GCC_NAME}"
        # Also drop the build dir
        rm -rf "$DIR/Build/$ARCH/$GCC_NAME"
    fi
    buildstep toolchain_dl printf "\n\tExtracting gcc...\n\n"
    tar -xJf $GCC_PKG
    pushd $GCC_NAME
        if [ "$git_patch" = "1" ]; then
            git init > /dev/null
            git add . > /dev/null
            git commit -am "BASE" > /dev/null
            git apply "$DIR"/Patches/gcc.patch > /dev/null
        else
            patch -p1 < "$DIR/Patches/gcc.patch" > /dev/null
        fi
        $SHA256SUM "$DIR/Patches/gcc.patch" > .patch.applied
    popd

    if [ "$SYSTEM_NAME" = "Darwin" ]; then
        pushd "gcc-${GCC_VERSION}"
        ./contrib/download_prerequisites
        popd
    fi
popd


# === COMPILE AND INSTALL ===

rm -rf "$PREFIX"
mkdir -p "$PREFIX"

if [ -z "$MAKEJOBS" ]; then
    MAKEJOBS=$($NPROC)
fi

mkdir -p "$DIR/Build/$ARCH"

pushd "$DIR/Build/$ARCH"
    unset PKG_CONFIG_LIBDIR # Just in case

    if [ "$ARCH" = "aarch64" ]; then
        rm -rf gdb
        mkdir -p gdb

        pushd gdb
            buildstep "gdb/configure" printf "\n\n\tXXX configure gdb\n\n"
            buildstep "gdb/configure" "$DIR"/Tarballs/$GDB_NAME/configure --prefix="$PREFIX" \
                                                     --target="$TARGET" \
                                                     --with-sysroot="$SYSROOT" \
                                                     --enable-shared \
                                                     --disable-nls \
                                                     ${TRY_USE_LOCAL_TOOLCHAIN:+"--quiet"} || exit 1
            buildstep "gdb/build" printf "\n\n\tXXX build gdb\n\n"
            buildstep "gdb/build" "$MAKE" -j "$MAKEJOBS" || exit 1
            buildstep "gdb/install" printf "\n\n\tXXX install gdb\n\n"
            buildstep "gdb/install" "$MAKE" install || exit 1
        popd
    fi

    rm -rf binutils
    mkdir -p binutils

    pushd binutils
        buildstep "binutils/configure" printf "\n\n\tXXX configure binutils\n\n"
        buildstep "binutils/configure" "$DIR"/Tarballs/$BINUTILS_NAME/configure --prefix="$PREFIX" \
                                                 --target="$TARGET" \
                                                 --with-sysroot="$SYSROOT" \
                                                 --enable-shared \
                                                 --disable-nls \
                                                 ${TRY_USE_LOCAL_TOOLCHAIN:+"--quiet"} || exit 1
        if [ "$SYSTEM_NAME" = "Darwin" ]; then
            # under macOS generated makefiles are not resolving the "intl"
            # dependency properly to allow linking its own copy of
            # libintl when building with --enable-shared.
            buildstep "binutils/build" "$MAKE" -j "$MAKEJOBS" || true
            pushd intl
            buildstep "binutils/build" "$MAKE" all-yes
            popd
        fi
        buildstep "binutils/build" printf "\n\n\tXXX build binutils\n\n"
        buildstep "binutils/build" "$MAKE" -j "$MAKEJOBS" || exit 1
        buildstep "binutils/install" "$MAKE" install || exit 1
    popd

    buildstep install_headers printf "\n\n\t\tXXX serenity libc, libm and libpthread headers\n"
    mkdir -p "$BUILD"
    pushd "$BUILD"
        mkdir -p Root/usr/include/
        SRC_ROOT=$($REALPATH "$DIR"/..)
        FILES=$(find "$SRC_ROOT"/Kernel/API "$SRC_ROOT"/Userland/Libraries/LibC "$SRC_ROOT"/Userland/Libraries/LibM "$SRC_ROOT"/Userland/Libraries/LibPthread -name '*.h' -print)
        for header in $FILES; do
            target=$(echo "$header" | sed -e "s@$SRC_ROOT/Userland/Libraries/LibC@@" -e "s@$SRC_ROOT/Userland/Libraries/LibM@@" -e "s@$SRC_ROOT/Userland/Libraries/LibPthread@@" -e "s@$SRC_ROOT/Kernel/@Kernel/@")
            buildstep "system_headers" $INSTALL -D "$header" "Root/usr/include/$target"
        done
        unset SRC_ROOT
    popd

    if [ "$SYSTEM_NAME" = "OpenBSD" ]; then
        perl -pi -e 's/-no-pie/-nopie/g' "$DIR/Tarballs/gcc-$GCC_VERSION/gcc/configure"
    fi

    if [ ! -f "$DIR/Tarballs/gcc-$GCC_VERSION/gcc/config/serenity-userland.h" ]; then
        cp "$DIR/Tarballs/gcc-$GCC_VERSION/gcc/config/serenity.h" "$DIR/Tarballs/gcc-$GCC_VERSION/gcc/config/serenity-kernel.h"
    fi

    rm -rf gcc
    mkdir -p gcc

    pushd gcc
        buildstep config_gcc_libgcc printf "\n\n\t\tXXX configure gcc and libgcc\n\n"
        buildstep "gcc/configure" "$DIR/Tarballs/gcc-$GCC_VERSION/configure" --prefix="$PREFIX" \
                                            --target="$TARGET" \
                                            --with-sysroot="$SYSROOT" \
                                            --disable-nls \
                                            --with-newlib \
                                            --enable-shared \
                                            --enable-languages=c,c++ \
                                            --enable-default-pie \
                                            --enable-lto \
                                            --enable-threads=posix \
                                            ${TRY_USE_LOCAL_TOOLCHAIN:+"--quiet"} || exit 1

        buildstep build_gcc_libgcc printf "\n\n\tXXX build gcc and libgcc\n\n"
        buildstep "gcc/build" "$MAKE" -j "$MAKEJOBS" all-gcc || exit 1
        if [ "$SYSTEM_NAME" = "OpenBSD" ]; then
            ln -sf liblto_plugin.so.0.0 gcc/liblto_plugin.so
        fi
        buildstep "libgcc/build" "$MAKE" -j "$MAKEJOBS" all-target-libgcc || exit 1
        buildstep "gcc+libgcc/install" printf "\n\n\tXXX install gcc and libgcc\n\n"
        buildstep "gcc+libgcc/install" "$MAKE" install-gcc install-target-libgcc || exit 1

        buildstep "libstdc++/build" printf "\n\n\tXXX build libstdc++\n\n"
        buildstep "libstdc++/build" "$MAKE" -j "$MAKEJOBS" all-target-libstdc++-v3 || exit 1
        buildstep "libstdc++/install" printf "\n\n\tXXX install libstdc++\n"
        buildstep "libstdc++/install" "$MAKE" install-target-libstdc++-v3 || exit 1
    popd

    if [ "$SYSTEM_NAME" = "OpenBSD" ]; then
        cd "$DIR/Local/${ARCH}/libexec/gcc/$TARGET/$GCC_VERSION" && ln -sf liblto_plugin.so.0.0 liblto_plugin.so
    fi
popd


# == SAVE TO CACHE ==

pushd "$DIR"
    if [ "${TRY_USE_LOCAL_TOOLCHAIN}" = "y" ] ; then
        buildstep save_cache printf "::endgroup::\n"
        buildstep save_cache printf "Building cache tar:\n"

        rm -f "${CACHED_TOOLCHAIN_ARCHIVE}"  # Just in case

        tar czf "${CACHED_TOOLCHAIN_ARCHIVE}" Local/

        buildstep save_cache printf "Cache (after):\n"
        ls -l Cache
    fi
popd
buildstep BuildIt_end printf "\n\n\t\t... Finished running %s from %s/ !\n\n\n" "${BASH_SOURCE[0]}" "$DIR"
