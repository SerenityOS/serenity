#!/usr/bin/env bash

# This script builds the mold linker that can optionally be used for linking
# the SerenityOS userland.
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

NPROC="nproc"
SYSTEM_NAME="$(uname -s)"

if [ "$SYSTEM_NAME" = "OpenBSD" ]; then
    NPROC="sysctl -n hw.ncpuonline"
elif [ "$SYSTEM_NAME" = "FreeBSD" ]; then
    NPROC="sysctl -n hw.ncpu"
elif [ "$SYSTEM_NAME" = "Darwin" ]; then
    NPROC="sysctl -n hw.ncpu"
fi

[ -z "$MAKEJOBS" ] && MAKEJOBS=$($NPROC)

mkdir -p "$DIR"/Tarballs
pushd "$DIR"/Tarballs

if [ "$1" = "--git" ]; then
    [ ! -d mold ] && git clone https://github.com/rui314/mold.git

    cd mold

    git pull
else
    VERSION=1.0.2
    [ ! -e mold-$VERSION.tar.gz ] && curl -L "https://github.com/rui314/mold/archive/refs/tags/v$VERSION.tar.gz" -o mold-$VERSION.tar.gz
    [ ! -e mold-$VERSION ] && tar -xzf mold-$VERSION.tar.gz
    cd mold-$VERSION
fi

make clean
export DESTDIR="$DIR"/Local/mold
export PREFIX=
make -j "$MAKEJOBS"
make install

popd
