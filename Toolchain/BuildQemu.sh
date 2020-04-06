#!/bin/bash
set -e

# This file will need to be run in bash, for now.

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "$DIR"

TARGET=i686-pc-serenity
PREFIX="$DIR/Local"
SYSROOT="$DIR/../Root"

QEMU300_MD5SUM="6a5c8df583406ea24ef25b239c3243e0"
QEMU410_MD5SUM="cdf2b5ca52b9abac9bacb5842fa420f8"

QEMU_VERSION="qemu-4.1.0"
QEMU_MD5SUM="${QEMU410_MD5SUM}"

echo PREFIX is "$PREFIX"
echo SYSROOT is "$SYSROOT"

mkdir -p "$DIR/Tarballs"

pushd "$DIR/Tarballs"
    if [ ! -e "$QEMU_VERSION.tar.xz" ]; then
        curl -O "https://download.qemu.org/$QEMU_VERSION.tar.xz"
    else
        echo "Skipped downloading $QEMU_VERSION"
    fi

    md5="$(md5sum $QEMU_VERSION.tar.xz | cut -f1 -d' ')"
    echo "qemu md5='$md5'"
    if  [ "$md5" != "$QEMU_MD5SUM" ] ; then
        echo "qemu md5 sum mismatching, please run script again."
        rm $$QEMU_VERSION.tar.xz
        exit 1
    fi

    if [ ! -d "$QEMU_VERSION" ]; then
        echo "Extracting qemu..."
        tar -xf "$QEMU_VERSION.tar.xz"
    else
        echo "Skipped extracting qemu"
    fi
popd

mkdir -p "$PREFIX"
mkdir -p "$DIR/Build/qemu"

if [ -z "$MAKEJOBS" ]; then
    MAKEJOBS=$(nproc)
fi

if [[ $(uname) == "Darwin" ]]
then
    UI_LIB=cocoa
else
    UI_LIB=gtk
fi

echo Using $UI_LIB based UI

pushd "$DIR/Build/"
    pushd qemu
        "$DIR"/Tarballs/$QEMU_VERSION/configure --prefix="$PREFIX" \
                                                --target-list=i386-softmmu \
                                                --enable-$UI_LIB || exit 1
        make -j "$MAKEJOBS" || exit 1
        make install || exit 1
    popd
popd
