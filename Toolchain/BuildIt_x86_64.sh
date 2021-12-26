#!/usr/bin/env bash
set -e

# This file will need to be run in bash, for now.

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "$DIR"

ARCH=${ARCH:-"x86_64"}
TARGET="$ARCH-pc-serenity"
PREFIX="$DIR/Local/x86_64"
SYSROOT="$DIR/../Root"

MAKE="make"
MD5SUM="md5sum"
NPROC="nproc"

if [ "$(uname -s)" = "OpenBSD" ]; then
    MAKE=gmake
    MD5SUM="md5 -q"
    NPROC="sysctl -n hw.ncpuonline"
    export CC=egcc
    export CXX=eg++
    export with_gmp=/usr/local
    export LDFLAGS=-Wl,-z,notext
elif [ "$(uname -s)" = "FreeBSD" ]; then
    MAKE=gmake
    MD5SUM="md5 -q"
    NPROC="sysctl -n hw.ncpu"
fi

echo PREFIX is "$PREFIX"
echo SYSROOT is "$SYSROOT"

mkdir -p "$DIR/Tarballs"

BINUTILS_VERSION="2.33.1"
BINUTILS_MD5SUM="1a6b16bcc926e312633fcc3fae14ba0a"
BINUTILS_NAME="binutils-$BINUTILS_VERSION"
BINUTILS_PKG="${BINUTILS_NAME}.tar.gz"
BINUTILS_BASE_URL="http://ftp.gnu.org/gnu/binutils"

GCC_VERSION="10.1.0"
GCC_MD5SUM="8a9fbd7e24d04c5d36e96bc894d3cd6b"
GCC_NAME="gcc-$GCC_VERSION"
GCC_PKG="${GCC_NAME}.tar.gz"
GCC_BASE_URL="http://ftp.gnu.org/gnu/gcc"

pushd "$DIR/Tarballs"
    md5="$($MD5SUM $BINUTILS_PKG | cut -f1 -d' ')"
    echo "bu md5='$md5'"
    if [ ! -e $BINUTILS_PKG ] || [ "$md5" != ${BINUTILS_MD5SUM} ] ; then
        rm -f $BINUTILS_PKG
        curl -LO "$BINUTILS_BASE_URL/$BINUTILS_PKG"
    else
        echo "Skipped downloading binutils"
    fi

    md5="$($MD5SUM ${GCC_PKG} | cut -f1 -d' ')"
    echo "gc md5='$md5'"
    if [ ! -e $GCC_PKG ] || [ "$md5" != ${GCC_MD5SUM} ] ; then
        rm -f $GCC_PKG
        curl -LO "$GCC_BASE_URL/$GCC_NAME/$GCC_PKG"
    else
        echo "Skipped downloading gcc"
    fi

    if [ ! -d ${BINUTILS_NAME} ]; then
        echo "Extracting binutils..."
        tar -xzf ${BINUTILS_PKG}

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
        tar -xzf $GCC_PKG

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

mkdir -p "$DIR/Build/x86_64/binutils"
mkdir -p "$DIR/Build/x86_64/gcc"

if [ -z "$MAKEJOBS" ]; then
    MAKEJOBS=$($NPROC)
fi

pushd "$DIR/Build/x86_64"
    unset PKG_CONFIG_LIBDIR # Just in case

    pushd binutils
        rm -f ./config.cache # Let's do this in case someone has already built the i686 version
        "$DIR"/Tarballs/binutils-2.33.1/configure --prefix="$PREFIX" \
                                                --target="$TARGET" \
                                                --with-sysroot="$SYSROOT" \
                                                --enable-shared \
                                                --disable-nls || exit 1
        if [ "$(uname)" = "Darwin" ]; then
            # under macOS generated makefiles are not resolving the "intl"
            # dependency properly to allow linking its own copy of
            # libintl when building with --enable-shared.
            "$MAKE" -j "$MAKEJOBS" || true
            pushd intl
            "$MAKE" all-yes
            popd
        fi
        "$MAKE" -j "$MAKEJOBS" || exit 1
        "$MAKE" install || exit 1
    popd

    pushd gcc
        if [ "$(uname -s)" = "OpenBSD" ]; then
            perl -pi -e 's/-no-pie/-nopie/g' "$DIR/Tarballs/gcc-$GCC_VERSION/gcc/configure"
        fi

        "$DIR/Tarballs/gcc-$GCC_VERSION/configure" --prefix="$PREFIX" \
                                            --target="$TARGET" \
                                            --with-sysroot="$SYSROOT" \
                                            --disable-nls \
                                            --with-newlib \
                                            --enable-shared \
                                            --enable-languages=c,c++ || exit 1

        echo "XXX build gcc and libgcc"
        "$MAKE" -j "$MAKEJOBS" all-gcc all-target-libgcc || exit 1
        echo "XXX install gcc and libgcc"
        "$MAKE" install-gcc install-target-libgcc || exit 1

        echo "XXX serenity libc and libm"
        ( cd "$DIR/../Libraries/LibC/" && "$MAKE" clean && "$MAKE" EXTRA_LIBC_DEFINES="-DBUILDING_SERENITY_TOOLCHAIN" && "$MAKE" install )
        ( cd "$DIR/../Libraries/LibM/" && "$MAKE" clean && "$MAKE" && "$MAKE" install )

        echo "XXX build libstdc++"
        "$MAKE" all-target-libstdc++-v3 || exit 1
        echo "XXX install libstdc++"
        "$MAKE" install-target-libstdc++-v3 || exit 1

        if [ "$(uname -s)" = "OpenBSD" ]; then
            cd "$DIR/Local/libexec/gcc/x86_64-pc-serenity/$GCC_VERSION" && ln -sf liblto_plugin.so.0.0 liblto_plugin.so
        fi
    popd
popd

