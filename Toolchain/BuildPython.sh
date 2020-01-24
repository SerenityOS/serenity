#!/bin/bash
set -e

# This file will need to be run in bash, for now.

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "$DIR"

TARGET=i686-pc-serenity
PREFIX="$DIR/Local"
SYSROOT="$DIR/../Root"

source "$DIR/../Ports/python-3.6/version.sh"

echo PYTHON_VERSION is "$PYTHON_VERSION"
echo PYTHON_URL is "$PYTHON_URL"

echo PREFIX is "$PREFIX"
echo SYSROOT is "$SYSROOT"

mkdir -p "$DIR/Tarballs"

pushd "$DIR/Tarballs"
    if [ ! -e "$PYTHON_ARCHIVE" ]; then
        curl -O "$PYTHON_URL"
    else
        echo "Skipped downloading Python-$PYTHON_VERSION"
    fi

    md5="$(md5sum $PYTHON_ARCHIVE | cut -f1 -d' ')"
    echo "python md5='$md5'"
    if  [ "$md5" != "$PYTHON_MD5SUM" ] ; then
        echo "python md5 sum mismatching, please run script again."
        rm $PYTHON_ARCHIVE
        exit 1
    fi

    if [ ! -d "Python-$PYTHON_VERSION" ]; then
        echo "Extracting python..."
        tar -xf "$PYTHON_ARCHIVE"
    else
        echo "Skipped extracting python"
    fi
popd

mkdir -p "$PREFIX"
mkdir -p "$DIR/Build/python"

if [ -z "$MAKEJOBS" ]; then
    MAKEJOBS=$(nproc)
fi

pushd "$DIR/Build/"
    pushd python
        "$DIR"/Tarballs/Python-$PYTHON_VERSION/configure --prefix="$PREFIX" || exit 1
        make -j "$MAKEJOBS" || exit 1
        make install || exit 1
    popd
popd
