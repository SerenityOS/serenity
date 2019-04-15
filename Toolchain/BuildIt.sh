#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

TARGET=i686-pc-serenity
PREFIX="$DIR/Local"

mkdir -p "$DIR/Tarballs"

pushd "$DIR/Tarballs"
    if [ ! -e "binutils-2.32.tar.gz" ]; then
        wget "http://ftp.gnu.org/gnu/binutils/binutils-2.32.tar.gz"
    fi

    if [ ! -e "gcc-8.3.0.tar.gz" ]; then
        wget "http://ftp.gnu.org/gnu/gcc/gcc-8.3.0/gcc-8.3.0.tar.gz"
    fi

    if [ ! -d "binutils-2.32" ]; then
        tar -xf "binutils-2.32.tar.gz"

        pushd "binutils-2.32"
            patch -p1 < $DIR/Patches/binutils.patch
        popd
    fi

    if [ ! -d "gcc-8.3.0" ]; then
        tar -xf "gcc-8.3.0.tar.gz"

        pushd "gcc-8.3.0"
            patch -p1 < $DIR/Patches/gcc.patch
        popd
    fi
popd

exit

mkdir -p $PREFIX

mkdir -p "$DIR/Build/binutils"
mkdir -p "$DIR/Build/gcc"

pushd "$DIR/Build/"

    unset PKG_CONFIG_LIBDIR # Just in case

    pushd binutils
        $DIR/Tarballs/binutils-2.32/configure --target=$TARGET --prefix=$PREFIX --disable-werror || exit 1
        make -j $(nproc)
        make install
    popd

    pushd gcc
        $DIR/Tarballs/gcc-8.3.0/configure --target=$TARGET --prefix=$PREFIX --disable-nls --enable-languages=c --disable-libssp --without-headers || exit 1
        make -j $(nproc) all-gcc all-target-libgcc
        make install-gcc install-target-libgcc
    popd
popd