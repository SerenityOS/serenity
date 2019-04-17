#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo $DIR

TARGET=i686-pc-serenity
PREFIX="$DIR/Local"
SYSROOT="$DIR/../Base"

mkdir -p "$DIR/Tarballs"

pushd "$DIR/Tarballs"
    if [ ! -e "binutils-2.32.tar.gz" ]; then
        wget "http://ftp.gnu.org/gnu/binutils/binutils-2.32.tar.gz"
    else
        echo "Skipped downloading binutils"
    fi

    if [ ! -e "gcc-8.3.0.tar.gz" ]; then
        wget "http://ftp.gnu.org/gnu/gcc/gcc-8.3.0/gcc-8.3.0.tar.gz"
    else
        echo "Skipped downloading gcc"
    fi

    if [ ! -d "binutils-2.32" ]; then
        echo "Extracting binutils..."
        tar -xf "binutils-2.32.tar.gz"

        pushd "binutils-2.32"
            patch -p1 < $DIR/Patches/binutils.patch > /dev/null
        popd
    else
        echo "Skipped extracting binutils"
    fi

    if [ ! -d "gcc-8.3.0" ]; then
        echo "Extracting gcc..."
        tar -xf "gcc-8.3.0.tar.gz"

        pushd "gcc-8.3.0"
            patch -p1 < $DIR/Patches/gcc.patch > /dev/null
        popd
    else
        echo "Skipped extracting gcc"
    fi
popd

mkdir -p $PREFIX

mkdir -p "$DIR/Build/binutils"
mkdir -p "$DIR/Build/gcc"

pushd "$DIR/Build/"
    unset PKG_CONFIG_LIBDIR # Just in case

    pushd binutils
        $DIR/Tarballs/binutils-2.32/configure --prefix=$PREFIX \
                                              --target=$TARGET \
                                              --with-sysroot=$SYSROOT \
                                              --disable-nls || exit 1
        make -j $(nproc)
        make install
    popd

    pushd gcc
        $DIR/Tarballs/gcc-8.3.0/configure --prefix=$PREFIX \
                                          --target=$TARGET \
                                          --with-sysroot=$SYSROOT \
                                          --enable-languages=c,c++ || exit 1

        make -j $(nproc) all-gcc all-target-libgcc
        make install-gcc install-target-libgcc

        make -c ../LibC/ install

        make all-target-libstdc++-v3
        make install-target-libstdc++-v3
    popd
popd