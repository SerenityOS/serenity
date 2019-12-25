#!/bin/bash
set -e

# This file will need to be run in bash, for now.

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "$DIR"

ARCH=${ARCH:-"i686"}
TARGET="$ARCH-pc-serenity"
PREFIX="$DIR/Local"
SYSROOT="$DIR/../Root"

echo PREFIX is "$PREFIX"
echo SYSROOT is "$SYSROOT"

mkdir -p "$DIR/Tarballs"

BINUTILS_VERSION="2.33.1"
BINUTILS_MD5SUM="1a6b16bcc926e312633fcc3fae14ba0a"
BINUTILS_NAME="binutils-$BINUTILS_VERSION"
BINUTILS_PKG="${BINUTILS_NAME}.tar.gz"
BINUTILS_BASE_URL="http://ftp.gnu.org/gnu/binutils"

GCC_VERSION="9.2.0"
GCC_MD5SUM="e03739b042a14376d727ddcfd05a9bc3"
GCC_NAME="gcc-$GCC_VERSION"
GCC_PKG="${GCC_NAME}.tar.gz"
GCC_BASE_URL="http://ftp.gnu.org/gnu/gcc"

pushd "$DIR/Tarballs"
    md5="$(md5sum $BINUTILS_PKG | cut -f1 -d' ')"
    echo "bu md5='$md5'"
    if [ ! -e $BINUTILS_PKG ] || [ "$md5" != ${BINUTILS_MD5SUM} ] ; then
        rm -f $BINUTILS_PKG
        wget "$BINUTILS_BASE_URL/$BINUTILS_PKG"
    else
        echo "Skipped downloading binutils"
    fi

    md5="$(md5sum ${GCC_PKG} | cut -f1 -d' ')"
    echo "gc md5='$md5'"
    if [ ! -e $GCC_PKG ] || [ "$md5" != ${GCC_MD5SUM} ] ; then
        rm -f $GCC_PKG
        wget "$GCC_BASE_URL/$GCC_NAME/$GCC_PKG"
    else
        echo "Skipped downloading gcc"
    fi

    if [ ! -d ${BINUTILS_NAME} ]; then
        echo "Extracting binutils..."
        tar -xf ${BINUTILS_PKG}

        pushd ${BINUTILS_NAME}
            git init >/dev/null
            git add . >/dev/null
            git commit -am "BASE" >/dev/null
            git apply "$DIR"/Patches/binutils.patch >/dev/null
        popd
    else
        echo "Skipped extracting binutils"
    fi

    if [ ! -d $GCC_NAME ]; then
        echo "Extracting gcc..."
        tar -xf $GCC_PKG

        pushd $GCC_NAME
            git init >/dev/null
            git add . >/dev/null
            git commit -am "BASE" >/dev/null
            git apply "$DIR"/Patches/gcc.patch >/dev/null
        popd
    else
        echo "Skipped extracting gcc"
    fi

    if [ "$(uname)" = "Darwin" ]; then
        pushd "gcc-${GCC_VERSION}"
        ./contrib/download_prerequisites
        popd
    fi

popd

mkdir -p "$PREFIX"

mkdir -p "$DIR/Build/binutils"
mkdir -p "$DIR/Build/gcc"

if [ -z "$MAKEJOBS" ]; then
    MAKEJOBS=$(nproc)
fi

pushd "$DIR/Build/"
    unset PKG_CONFIG_LIBDIR # Just in case

    pushd binutils
        "$DIR"/Tarballs/binutils-2.33.1/configure --prefix="$PREFIX" \
                                                --target="$TARGET" \
                                                --with-sysroot="$SYSROOT" \
                                                --disable-nls || exit 1
        make -j "$MAKEJOBS" || exit 1
        make install || exit 1
    popd

    pushd gcc
        "$DIR"/Tarballs/gcc-9.2.0/configure --prefix="$PREFIX" \
                                            --target="$TARGET" \
                                            --with-sysroot="$SYSROOT" \
                                            --disable-nls \
                                            --with-newlib \
                                            --enable-languages=c,c++ || exit 1

        echo "XXX build gcc and libgcc"
        make -j "$MAKEJOBS" all-gcc all-target-libgcc || exit 1
        echo "XXX install gcc and libgcc"
        make install-gcc install-target-libgcc || exit 1

        echo "XXX serenity libc and libm"
        ( cd "$DIR/../Libraries/LibC/" && make clean && make && make install )
        ( cd "$DIR/../Libraries/LibM/" && make clean && make && make install )

        echo "XXX build libstdc++"
        make all-target-libstdc++-v3 || exit 1 
        echo "XXX install libstdc++"
        make install-target-libstdc++-v3 || exit 1
    popd
popd

