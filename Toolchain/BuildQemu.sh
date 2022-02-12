#!/usr/bin/env bash
set -e

# This file will need to be run in bash, for now.

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "$DIR"

PREFIX="$DIR/Local/qemu"
BUILD=$(realpath "$DIR/../Build")
SYSROOT="$BUILD/Root"

QEMU_VERSION="qemu-6.2.0"
QEMU_MD5SUM="a077669ce58b6ee07ec355e54aad25be"

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
        rm -f $QEMU_VERSION.tar.xz
        exit 1
    fi

    if [ ! -d "$QEMU_VERSION" ]; then
        echo "Extracting qemu..."
        tar -xf "$QEMU_VERSION.tar.xz"
    else
        echo "Skipped extracting qemu"
    fi

    pushd "$QEMU_VERSION"
        patch -p1 < "$DIR/Patches/qemu-cf-protection-none.patch" > /dev/null
        md5sum "$DIR/Patches/qemu-cf-protection-none.patch" > .patch.applied
    popd

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
                                            --target-list=aarch64-softmmu,i386-softmmu,x86_64-softmmu \
                                            --enable-$UI_LIB || exit 1
    make -j "$MAKEJOBS" || exit 1
    make install || exit 1
popd
