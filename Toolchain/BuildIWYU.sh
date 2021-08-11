#!/usr/bin/env bash
set -eo pipefail

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

ARCH=${ARCH:-"i686"}

BUILD="$DIR/Build/iwyu/$ARCH"
PREFIX="$DIR/Local/iwyu/$ARCH"


IWYU_VERSION=0.16
IWYU_NAME="include-what-you-use-$IWYU_VERSION"
IWYU_PKG="$IWYU_NAME.tar.gz"
IWYU_URL="https://github.com/include-what-you-use/include-what-you-use/archive/refs/tags/$IWYU_VERSION.tar.gz"


pushd "$DIR/Tarballs"
    if [ -e "$IWYU_PKG" ]; then
        echo "Skipped downloading IWYU"
    else
        rm -f "$IWYU_PKG"
        curl -L "$IWYU_URL" -o "$IWYU_PKG"
    fi

    if [ -d "$IWYU_NAME" ]; then
        rm -rf "$IWYU_NAME"
        rm -rf "$BUILD/iwyu/$ARCH"
    fi

    echo "Extracting IWYU..."
    tar -xzf "$IWYU_PKG"
popd

mkdir -p "$BUILD"
pushd "$BUILD"
    cmake "$DIR/Tarballs/$IWYU_NAME" \
        -GNinja \
        -DCMAKE_PREFIX_PATH="$DIR/Local/clang/$ARCH/lib/" \
        -DIWYU_LINK_CLANG_DYLIB=ON \
        -DCMAKE_INSTALL_PREFIX="$PREFIX" || exit 1

    ninja || exit 1
    ninja install || exit 1
popd
