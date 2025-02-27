#!/usr/bin/env bash
set -eo pipefail
# This file will need to be run in bash, for now.

# === CONFIGURATION AND SETUP ===

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# shellcheck source=/dev/null
. "${DIR}/../Meta/shell_include.sh"

exit_if_running_as_root "Do not run BuildGNU.sh as root, your Build directory will become root-owned"

echo "$DIR"

ARCH=${ARCH:-"x86_64"}
TARGET="$ARCH-pc-serenity"
PREFIX="$DIR/Local/$ARCH"
BUILD="$DIR/../Build/$ARCH"
SYSROOT="$BUILD/Root"

MAKE="make"
MD5SUM="md5sum"
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
    MD5SUM="md5 -q"
    REALPATH="readlink -f"
    export CC=egcc
    export CXX=eg++
    export with_gmp=/usr/local
    export LDFLAGS=-Wl,-z,notext
elif [ "$SYSTEM_NAME" = "FreeBSD" ]; then
    MAKE=gmake
    MD5SUM="md5 -q"
    export with_gmp=/usr/local
    export with_mpfr=/usr/local
elif [ "$SYSTEM_NAME" = "Darwin" ]; then
    MD5SUM="md5 -q"
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

echo PREFIX is "$PREFIX"
echo SYSROOT is "$SYSROOT"

mkdir -p "$DIR/Tarballs"

BINUTILS_VERSION="2.41"
BINUTILS_MD5SUM="256d7e0ad998e423030c84483a7c1e30"
BINUTILS_NAME="binutils-$BINUTILS_VERSION"
BINUTILS_PKG="${BINUTILS_NAME}.tar.xz"
BINUTILS_BASE_URL="https://ftpmirror.gnu.org/gnu/binutils"

# Note: If you bump the gcc version, you also have to update the matching
#       GCC_VERSION variable in the project's root CMakeLists.txt
GCC_VERSION="13.2.0"
GCC_MD5SUM="e0e48554cc6e4f261d55ddee9ab69075"
GCC_NAME="gcc-$GCC_VERSION"
GCC_PKG="${GCC_NAME}.tar.xz"
GCC_BASE_URL="https://ftpmirror.gnu.org/gnu/gcc"

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

buildstep dependencies echo "Checking whether 'patch' is available..."
if ! command -v patch >/dev/null; then
    buildstep dependencies echo "Please make sure to install GNU patch (for the 'patch' tool)."
    exit 1
fi

buildstep dependencies echo "Checking whether your C compiler works..."
if ! ${CC:-cc} -o /dev/null -xc - >/dev/null <<'PROGRAM'
int main() {}
PROGRAM
then
    buildstep dependencies echo "Please make sure to install a working C compiler."
    exit 1
fi

if [ "$SYSTEM_NAME" != "Darwin" ]; then
    for lib in gmp mpc mpfr; do
        buildstep dependencies echo "Checking whether the $lib library and headers are available..."
        if ! ${CC:-cc} -I /usr/local/include -L /usr/local/lib -l$lib -o /dev/null -xc - >/dev/null <<PROGRAM
#include <$lib.h>
int main() {}
PROGRAM
        then
            echo "Please make sure to install the $lib library and headers."
            exit 1
        fi
    done
fi

# === DOWNLOAD AND PATCH ===
pushd "$DIR/Tarballs"
    md5=""
    if [ -e "$BINUTILS_PKG" ]; then
        md5="$($MD5SUM $BINUTILS_PKG | cut -f1 -d' ')"
        echo "binutils md5='$md5'"
    fi
    if [ "$md5" != ${BINUTILS_MD5SUM} ] ; then
        rm -f $BINUTILS_PKG
        curl -LO "$BINUTILS_BASE_URL/$BINUTILS_PKG"
    else
        echo "Skipped downloading binutils"
    fi

    md5=""
    if [ -e "$GCC_PKG" ]; then
        md5="$($MD5SUM ${GCC_PKG} | cut -f1 -d' ')"
        echo "gcc md5='$md5'"
    fi
    if [ "$md5" != ${GCC_MD5SUM} ] ; then
        rm -f $GCC_PKG
        curl -LO "$GCC_BASE_URL/$GCC_NAME/$GCC_PKG"
    else
        echo "Skipped downloading gcc"
    fi

    patch_md5="$(${MD5SUM} "${DIR}"/Patches/binutils/*.patch)"

    if [ ! -d "${BINUTILS_NAME}" ] || [ "$(cat ${BINUTILS_NAME}/.patch.applied)" != "${patch_md5}" ]; then
        if [ -d ${BINUTILS_NAME} ]; then
            rm -rf "${BINUTILS_NAME}"
            rm -rf "${DIR}/Build/${ARCH}/${BINUTILS_NAME}"
        fi
        echo "Extracting binutils..."
        tar -xJf ${BINUTILS_PKG}

        pushd ${BINUTILS_NAME}
            if [ "${git_patch}" = "1" ]; then
                git init > /dev/null
                git add . > /dev/null
                git commit -am "BASE" > /dev/null
                git am "${DIR}"/Patches/binutils/*.patch > /dev/null
            else
                for patch in "${DIR}"/Patches/binutils/*.patch; do
                    patch -p1 < "${patch}" > /dev/null
                done
            fi
            ${MD5SUM} "${DIR}"/Patches/binutils/*.patch > .patch.applied
        popd
    else
        echo "Using existing binutils source directory"
    fi


    patch_md5="$(${MD5SUM} "${DIR}"/Patches/gcc/*.patch)"

    if [ ! -d "${GCC_NAME}" ] || [ "$(cat ${GCC_NAME}/.patch.applied)" != "${patch_md5}" ]; then
        if [ -d ${GCC_NAME} ]; then
            rm -rf "${GCC_NAME}"
            rm -rf "${DIR}/Build/${ARCH}/${GCC_NAME}"
        fi
        echo "Extracting gcc..."
        tar -xJf ${GCC_PKG}

        pushd ${GCC_NAME}
            if [ "${git_patch}" = "1" ]; then
                git init > /dev/null
                git add . > /dev/null
                git commit -am "BASE" > /dev/null
                git am --keep-non-patch "${DIR}"/Patches/gcc/*.patch > /dev/null
            else
                for patch in "${DIR}"/Patches/gcc/*.patch; do
                    patch -p1 < "${patch}" > /dev/null
                done
            fi
            ${MD5SUM} "${DIR}"/Patches/gcc/*.patch > .patch.applied

            if [ "${SYSTEM_NAME}" = "Darwin" ]; then
                ./contrib/download_prerequisites
            fi
        popd
    else
        echo "Using existing GCC source directory"
    fi
popd


# === COMPILE AND INSTALL ===

rm -rf "$PREFIX"
mkdir -p "$PREFIX"

NPROC=$(get_number_of_processing_units)
[ -z "$MAKEJOBS" ] && MAKEJOBS=${NPROC}

mkdir -p "$DIR/Build/$ARCH"

pushd "$DIR/Build/$ARCH"
    unset PKG_CONFIG_LIBDIR # Just in case

    rm -rf binutils
    mkdir -p binutils

    pushd binutils
        echo "XXX configure binutils"

        # We don't need the documentation that is being built, so
        # don't force people to install makeinfo just for that.
        export ac_cv_prog_MAKEINFO=true

        buildstep "binutils/configure" "$DIR"/Tarballs/$BINUTILS_NAME/configure --prefix="$PREFIX" \
                                                 --target="$TARGET" \
                                                 --with-sysroot="$SYSROOT" \
                                                 --enable-static \
                                                 --disable-shared \
                                                 --disable-nls \
                                                 ${CI:+"--quiet"} || exit 1
        echo "XXX build binutils"
        buildstep "binutils/build" "$MAKE" MAKEINFO=true -j "$MAKEJOBS" || exit 1
        buildstep "binutils/install" "$MAKE" MAKEINFO=true install || exit 1
    popd

    echo "XXX serenity libc headers"
    mkdir -p "$BUILD"
    pushd "$BUILD"
        mkdir -p Root/usr/include/
        SRC_ROOT=$($REALPATH "$DIR"/..)
        FILES=$(find \
            "$SRC_ROOT"/Kernel/API \
            "$SRC_ROOT"/Kernel/Arch \
            "$SRC_ROOT"/Userland/Libraries/LibC \
            "$SRC_ROOT"/Userland/Libraries/LibELF/ELFABI.h \
            "$SRC_ROOT"/Userland/Libraries/LibRegex/RegexDefs.h \
            -name '*.h' -print)
        for header in $FILES; do
            target=$(echo "$header" | sed \
                -e "s|$SRC_ROOT/Userland/Libraries/LibC||" \
                -e "s|$SRC_ROOT/Kernel/|Kernel/|" \
                -e "s|$SRC_ROOT/Userland/Libraries/LibELF/|LibELF/|" \
                -e "s|$SRC_ROOT/Userland/Libraries/LibRegex/|LibRegex/|")
            buildstep "system_headers" mkdir -p "$(dirname "Root/usr/include/$target")"
            buildstep "system_headers" $INSTALL "$header" "Root/usr/include/$target"
        done
        unset SRC_ROOT
    popd

    if [ "$SYSTEM_NAME" = "OpenBSD" ]; then
        perl -pi -e 's/-no-pie/-nopie/g' "$DIR/Tarballs/gcc-$GCC_VERSION/gcc/configure"
    fi

    rm -rf gcc
    mkdir -p gcc

    pushd gcc
        echo "XXX configure gcc and libgcc"
        buildstep "gcc/configure" "$DIR/Tarballs/gcc-$GCC_VERSION/configure" --prefix="$PREFIX" \
                                            --target="$TARGET" \
                                            --with-sysroot="$SYSROOT" \
                                            --disable-nls \
                                            --disable-libstdcxx-pch \
                                            --enable-static \
                                            --disable-shared \
                                            --enable-languages=c,c++,objc,obj-c++ \
                                            --enable-default-pie \
                                            --enable-lto \
                                            --enable-threads=posix \
                                            --enable-initfini-array \
                                            --with-linker-hash-style=gnu \
                                            ${CI:+"--quiet"} || exit 1

        echo "XXX build gcc and libgcc"
        buildstep "gcc/build" "$MAKE" -j "$MAKEJOBS" all-gcc || exit 1
        buildstep "libgcc/build" "$MAKE" -j "$MAKEJOBS" all-target-libgcc || exit 1
        echo "XXX install gcc and libgcc"
        buildstep "gcc+libgcc/install" "$MAKE" install-gcc install-target-libgcc || exit 1

        echo "XXX build libstdc++"
        buildstep "libstdc++/build" "$MAKE" -j "$MAKEJOBS" all-target-libstdc++-v3 || exit 1
        echo "XXX install libstdc++"
        buildstep "libstdc++/install" "$MAKE" install-target-libstdc++-v3 || exit 1
    popd

popd

pushd "$DIR/Local/$ARCH/$ARCH-pc-serenity/bin"
    buildstep "mold_symlink" ln -s ../../../mold/bin/mold ld.mold
popd
