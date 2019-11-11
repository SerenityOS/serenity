#!/bin/bash
set -e

# This file will need to be run in bash, for now.

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "$DIR"

TARGET=i686-pc-serenity
PREFIX="$DIR/Local"
SYSROOT="$DIR/../Root"

echo PREFIX is "$PREFIX"
echo SYSROOT is "$SYSROOT"

mkdir -p "$DIR/Tarballs"

source "$DIR/UseIt.sh"

pushd "$DIR/Tarballs"
    md5="$(md5sum binutils-2.32.tar.gz | cut -f1 -d' ')"
    echo "bu md5='$md5'"
    if [ ! -e "binutils-2.32.tar.gz" ] || [ "$md5" != "d1119c93fc0ed3007be4a84dd186af55" ] ; then
        rm -f binutils-2.32.tar.gz
        curl -O "http://ftp.gnu.org/gnu/binutils/binutils-2.32.tar.gz"
    else
        echo "Skipped downloading binutils"
    fi

    md5="$(md5sum gcc-8.3.0.tar.gz | cut -f1 -d' ')"
    echo "gc md5='$md5'"
    if [ ! -e "gcc-8.3.0.tar.gz" ] || [ "$md5" != "9972f8c24c02ebcb5a342c1b30de69ff" ] ; then
        rm -f gcc-8.3.0.tar.gz
        curl -O "http://ftp.gnu.org/gnu/gcc/gcc-8.3.0/gcc-8.3.0.tar.gz"
    else
        echo "Skipped downloading gcc"
    fi

    if [ ! -d "binutils-2.32" ]; then
        echo "Extracting binutils..."
        tar -xf "binutils-2.32.tar.gz"

        pushd "binutils-2.32"
            patch -p1 < "$DIR"/Patches/binutils.patch > /dev/null
        popd
    else
        echo "Skipped extracting binutils"
    fi

    if [ ! -d "gcc-8.3.0" ]; then
        echo "Extracting gcc..."
        tar -xf "gcc-8.3.0.tar.gz"

        pushd "gcc-8.3.0"
            patch -p1 < "$DIR"/Patches/gcc.patch > /dev/null
        popd
    else
        echo "Skipped extracting gcc"
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
        "$DIR"/Tarballs/binutils-2.32/configure --prefix="$PREFIX" \
                                                --target="$TARGET" \
                                                --with-sysroot="$SYSROOT" \
                                                --disable-nls || exit 1
        make -j "$MAKEJOBS" || exit 1
        make install || exit 1
    popd

    pushd gcc
        "$DIR"/Tarballs/gcc-8.3.0/configure --prefix="$PREFIX" \
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
        ( cd "$DIR/../Libraries/LibC/" && make clean && make && ./install.sh )
        ( cd "$DIR/../Libraries/LibM/" && make clean && make && ./install.sh )

        echo "XXX build libstdc++"
        make all-target-libstdc++-v3 || exit 1 
        echo "XXX install libstdc++"
        make install-target-libstdc++-v3 || exit 1
    popd
popd

