#!/usr/bin/env bash
set -e
# This file will need to be run in bash, for now.


# === CONFIGURATION AND SETUP ===

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "$DIR"

ARCH=${ARCH:-"i686"}
TARGET="$ARCH-pc-serenity"
PREFIX="$DIR/Local"
SYSROOT="$DIR/../Root"

MAKE=make
MD5SUM=md5sum
NPROC=nproc

if [ `uname -s` = "OpenBSD" ]; then
    MAKE=gmake
    MD5SUM="md5 -q"
    NPROC="sysctl -n hw.ncpuonline"
    export CC=egcc
    export CXX=eg++
    export with_gmp=/usr/local
    export LDFLAGS=-Wl,-z,notext
fi

echo PREFIX is "$PREFIX"
echo SYSROOT is "$SYSROOT"

mkdir -p "$DIR/Tarballs"

BINUTILS_VERSION="2.33.1"
BINUTILS_MD5SUM="1a6b16bcc926e312633fcc3fae14ba0a"
BINUTILS_NAME="binutils-$BINUTILS_VERSION"
BINUTILS_PKG="${BINUTILS_NAME}.tar.gz"
BINUTILS_BASE_URL="http://ftp.gnu.org/gnu/binutils"

GCC_VERSION="9.2.0"
GCC_MD5SUM="e03739b042a14376d727ddcfd05a9bc3"
GCC_NAME="gcc-$GCC_VERSION"
GCC_PKG="${GCC_NAME}.tar.gz"
GCC_BASE_URL="http://ftp.gnu.org/gnu/gcc"


# === CHECK CACHE AND REUSE ===

pushd "$DIR"
    if [ "${TRY_USE_LOCAL_TOOLCHAIN}" = "y" ] ; then
        echo "Checking cached toolchain:"

        DEPS_CONFIG="
            uname=$(uname),TARGET=${TARGET},
            BuildItHash=$($MD5SUM $(basename $0)),
            MAKE=${MAKE},MD5SUM=${MD5SUM},NPROC=${NPROC},
            CC=${CC},CXX=${CXX},with_gmp=${with_gmp},LDFLAGS=${LDFLAGS},
            BINUTILS_VERSION=${BINUTILS_VERSION},BINUTILS_MD5SUM=${BINUTILS_MD5SUM},
            GCC_VERSION=${GCC_VERSION},GCC_MD5SUM=${GCC_MD5SUM}"
        echo "Config is:${DEPS_CONFIG}"
        if ! DEPS_HASH=$($DIR/ComputeDependenciesHash.sh $MD5SUM <<<"${DEPS_CONFIG}"); then
            echo "Dependency hashing failed"
            echo "Will rebuild toolchain from scratch, and NOT SAVE THE RESULT."
            echo "Someone should look into this, but for now it'll work, albeit inefficient."
            # Should be empty anyway, but just to make sure:
            DEPS_HASH=""
        elif [ -r "Cache/ToolchainLocal_${DEPS_HASH}.tar.gz" ] ; then
            echo "Cache at Cache/ToolchainLocal_${DEPS_HASH}.tar.gz exists!"
            echo "Extracting toolchain from cache:"
            tar xzf "Cache/ToolchainLocal_${DEPS_HASH}.tar.gz"
            echo "Done 'building' the toolchain."
            exit 0
        else
            echo "Cache at Cache/ToolchainLocal_${DEPS_HASH}.tar.gz does not exist."
            echo "Will rebuild toolchain from scratch, and save the result."
        fi

    fi
popd


# === DOWNLOAD AND PATCH ===

pushd "$DIR/Tarballs"
    md5="$($MD5SUM $BINUTILS_PKG | cut -f1 -d' ')"
    echo "bu md5='$md5'"
    if [ ! -e $BINUTILS_PKG ] || [ "$md5" != ${BINUTILS_MD5SUM} ] ; then
        rm -f $BINUTILS_PKG
        wget "$BINUTILS_BASE_URL/$BINUTILS_PKG"
    else
        echo "Skipped downloading binutils"
    fi

    md5="$($MD5SUM ${GCC_PKG} | cut -f1 -d' ')"
    echo "gc md5='$md5'"
    if [ ! -e $GCC_PKG ] || [ "$md5" != ${GCC_MD5SUM} ] ; then
        rm -f $GCC_PKG
        wget "$GCC_BASE_URL/$GCC_NAME/$GCC_PKG"
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


# === COMPILE AND INSTALL ===

mkdir -p "$PREFIX"
mkdir -p "$DIR/Build/binutils"
mkdir -p "$DIR/Build/gcc"

if [ -z "$MAKEJOBS" ]; then
    MAKEJOBS=$($NPROC)
fi

pushd "$DIR/Build/"
    unset PKG_CONFIG_LIBDIR # Just in case

    pushd binutils
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
        if [ `uname -s` = "OpenBSD" ]; then
            perl -pi -e 's/-no-pie/-nopie/g' "$DIR"/Tarballs/gcc-9.2.0/gcc/configure
        fi

        "$DIR"/Tarballs/gcc-9.2.0/configure --prefix="$PREFIX" \
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

        if [ `uname -s` = "OpenBSD" ]; then
            cd "$DIR"/Local/libexec/gcc/i686-pc-serenity/9.2.0 && ln -sf liblto_plugin.so.0.0 liblto_plugin.so
        fi
    popd
popd


# == SAVE TO CACHE ==

pushd "$DIR"
    if [ "${TRY_USE_LOCAL_TOOLCHAIN}" = "y" ] ; then
        # TODO: Compress with -z.  It's factor 3, and costs no time.
        echo "Caching toolchain:"

        if [ -z "${DEPS_HASH}" ] ; then
            echo "NOT SAVED, because hashing failed."
            echo "It's computed in the beginning; see there for the error message."
        elif [ -e "Cache/ToolchainLocal_${DEPS_HASH}.tar.gz" ] ; then
            # Note: This checks for *existence*.  Initially we checked for
            # *readability*. If Travis borks permissions, there's not much we can do.
            echo "Cache exists but was not used?!"
            echo "Not touching cache then."
        else
            mkdir -p Cache/
            tar czf "Cache/ToolchainLocal_${DEPS_HASH}.tar.gz" Local/
        fi
    fi
popd
