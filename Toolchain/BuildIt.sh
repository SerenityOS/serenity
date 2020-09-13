#!/usr/bin/env bash
set -e
# This file will need to be run in bash, for now.


# === CONFIGURATION AND SETUP ===

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "$DIR"

ARCH=${ARCH:-"i686"}
TARGET="$ARCH-pc-serenity"
PREFIX="$DIR/Local"
BUILD=$(realpath "$DIR/../Build")
SYSROOT="$BUILD/Root"

MAKE="make"
MD5SUM="md5sum"
NPROC="nproc"

# Each cache entry is 70 MB. 5 entries are 350 MiB.
# It seems that Travis starts having trouble around a total
# cache size of 9 GiB, so I think this is a good amount.
KEEP_CACHE_COUNT=5

if command -v ginstall &>/dev/null; then
    INSTALL=ginstall
else
    INSTALL=install
fi

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

git_patch=
while [ "$1" != "" ]; do
    case $1 in
        --dev )           git_patch=1
                          ;;
    esac
    shift
done

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


# === CHECK CACHE AND REUSE ===

pushd "$DIR"
    if [ "${TRY_USE_LOCAL_TOOLCHAIN}" = "y" ] ; then
        echo "Checking cached toolchain:"
        # TODO: This is still overly pessimistic.
        DEPS_CONFIG="\
            uname=$(uname),TARGET=${TARGET},
            BuildItHash=$($MD5SUM "$(basename "$0")"),
            MAKE=${MAKE},MD5SUM=${MD5SUM},NPROC=${NPROC},
            CC=${CC},CXX=${CXX},with_gmp=${with_gmp},LDFLAGS=${LDFLAGS},
            BINUTILS_VERSION=${BINUTILS_VERSION},BINUTILS_MD5SUM=${BINUTILS_MD5SUM},
            GCC_VERSION=${GCC_VERSION},GCC_MD5SUM=${GCC_MD5SUM}"
        if ! DEPS_HASH=$("$DIR/ComputeDependenciesHash.sh" "$MD5SUM" <<<"${DEPS_CONFIG}"); then
            # Make it stand out more
            echo
            echo
            echo
            echo
            echo "Dependency hashing failed"
            echo "Will rebuild toolchain from scratch, and NOT SAVE THE RESULT."
            echo "Someone should look into this, but for now it'll work, albeit inefficient."
            echo
            echo
            echo
            echo
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
            echo "But first, getting rid of old, outdated caches. Current caches:"
            pushd "Cache/"
                ls -l
                # Travis preserves timestamps. Don't ask me why, but it does.
                # We can exploit this to get an easy approximation of recent-ness.
                # Our purging algorithm is simple: keep only the newest X entries.
                # Note that `find` doesn't easily support ordering by date,
                # and we control the filenames anyway.
                # shellcheck disable=SC2012
                ls -t | tail "-n+${KEEP_CACHE_COUNT}" | xargs -r rm -v
                echo "After deletion:"
                ls -l
            popd
        fi
    fi
popd


# === DOWNLOAD AND PATCH ===

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
            if [ "$git_patch" = "1" ]; then
                git init > /dev/null
                git add . > /dev/null
                git commit -am "BASE" > /dev/null
                git apply "$DIR"/Patches/binutils.patch > /dev/null
            else
                patch -p1 < "$DIR"/Patches/binutils.patch > /dev/null
            fi
        popd
    else
        echo "Skipped extracting binutils"
    fi

    if [ ! -d $GCC_NAME ]; then
        echo "Extracting gcc..."
        tar -xzf $GCC_PKG
        pushd $GCC_NAME
            if [ "$git_patch" = "1" ]; then
                git init > /dev/null
                git add . > /dev/null
                git commit -am "BASE" > /dev/null
                git apply "$DIR"/Patches/gcc.patch > /dev/null
            else
                patch -p1 < "$DIR/Patches/gcc.patch" > /dev/null
            fi
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
        echo "XXX configure binutils"
        "$DIR"/Tarballs/binutils-2.33.1/configure --prefix="$PREFIX" \
                                                --target="$TARGET" \
                                                --with-sysroot="$SYSROOT" \
                                                --enable-shared \
                                                --disable-nls \
                                                ${TRY_USE_LOCAL_TOOLCHAIN:+"--quiet"} || exit 1
        if [ "$(uname)" = "Darwin" ]; then
            # under macOS generated makefiles are not resolving the "intl"
            # dependency properly to allow linking its own copy of
            # libintl when building with --enable-shared.
            "$MAKE" -j "$MAKEJOBS" || true
            pushd intl
            "$MAKE" all-yes
            popd
        fi
        echo "XXX build binutils"
        "$MAKE" -j "$MAKEJOBS" || exit 1
        "$MAKE" install || exit 1
    popd

    pushd gcc
        if [ "$(uname -s)" = "OpenBSD" ]; then
            perl -pi -e 's/-no-pie/-nopie/g' "$DIR/Tarballs/gcc-$GCC_VERSION/gcc/configure"
        fi

        echo "XXX configure gcc and libgcc"
        "$DIR/Tarballs/gcc-$GCC_VERSION/configure" --prefix="$PREFIX" \
                                            --target="$TARGET" \
                                            --with-sysroot="$SYSROOT" \
                                            --disable-nls \
                                            --with-newlib \
                                            --enable-shared \
                                            --enable-languages=c,c++ \
                                            ${TRY_USE_LOCAL_TOOLCHAIN:+"--quiet"} || exit 1

        echo "XXX build gcc and libgcc"
        "$MAKE" -j "$MAKEJOBS" all-gcc all-target-libgcc || exit 1
        echo "XXX install gcc and libgcc"
        "$MAKE" install-gcc install-target-libgcc || exit 1

        echo "XXX serenity libc and libm headers"
        mkdir -p "$BUILD"
        pushd "$BUILD"
            mkdir -p Root/usr/include/
            SRC_ROOT=$(realpath "$DIR"/..)
            FILES=$(find "$SRC_ROOT"/Libraries/LibC "$SRC_ROOT"/Libraries/LibM -name '*.h' -print)
            for header in $FILES; do
                target=$(echo "$header" | sed -e "s@$SRC_ROOT/Libraries/LibC@@" -e "s@$SRC_ROOT/Libraries/LibM@@")
                $INSTALL -D "$header" "Root/usr/include/$target"
            done
            unset SRC_ROOT
        popd

        echo "XXX build libstdc++"
        "$MAKE" -j "$MAKEJOBS" all-target-libstdc++-v3 || exit 1
        echo "XXX install libstdc++"
        "$MAKE" install-target-libstdc++-v3 || exit 1

        if [ "$(uname -s)" = "OpenBSD" ]; then
            cd "$DIR/Local/libexec/gcc/i686-pc-serenity/$GCC_VERSION" && ln -sf liblto_plugin.so.0.0 liblto_plugin.so
        fi

    popd
popd


# == SAVE TO CACHE ==

pushd "$DIR"
    if [ "${TRY_USE_LOCAL_TOOLCHAIN}" = "y" ] ; then
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
            # We *most definitely* don't need debug symbols in the linker/compiler.
            # This cuts the uncompressed size from 1.2 GiB per Toolchain down to about 190 MiB.
            echo "Before: $(du -sh Local)"
            find Local/ -type f -executable ! -name '*.la' ! -name '*.sh' ! -name 'mk*' -exec strip {} +
            echo "After: $(du -sh Local)"
            tar czf "Cache/ToolchainLocal_${DEPS_HASH}.tar.gz" Local/
        fi
    fi
popd
