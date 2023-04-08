#!/usr/bin/env bash
set -e

# This file will need to be run in bash, for now.

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "$DIR"

PREFIX="$DIR/Local/qemu"
BUILD=$(realpath "$DIR/../Build")
SYSROOT="$BUILD/Root"

QEMU_VERSION=${QEMU_VERSION:="qemu-7.1.0"}
QEMU_MD5SUM=${QEMU_MD5SUM:="3be5458a9171b4ec5220c65d5d52bdcf"}

echo PREFIX is "$PREFIX"
echo SYSROOT is "$SYSROOT"

mkdir -p "$DIR/Tarballs"

pushd "$DIR/Tarballs"
    md5="$(md5sum $QEMU_VERSION.tar.xz | cut -f1 -d' ')"
    if [ ! -e "$QEMU_VERSION.tar.xz" ] || [ "$md5" != "$QEMU_MD5SUM" ]; then
        curl -C - -O "https://download.qemu.org/$QEMU_VERSION.tar.xz"
    else
        echo "Skipped downloading $QEMU_VERSION"
    fi

    md5="$(md5sum $QEMU_VERSION.tar.xz | cut -f1 -d' ')"
    echo "qemu md5='$md5'"
    if  [ "$md5" != "$QEMU_MD5SUM" ] ; then
        echo "qemu md5 sum mismatching, please run script again."
        rm -f $QEMU_VERSION.tar.xz
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

pushd "$DIR/Build/qemu"
    "$DIR"/Tarballs/$QEMU_VERSION/configure --prefix="$PREFIX" \
                                            --target-list=aarch64-softmmu,x86_64-softmmu \
                                            --enable-$UI_LIB || exit 1
    make -j "$MAKEJOBS" || exit 1
    make install || exit 1
popd
