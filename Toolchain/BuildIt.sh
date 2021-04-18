#!/usr/bin/env bash
set -e
# This file will need to be run in bash, for now.


# === CONFIGURATION AND SETUP ===

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "$DIR"

ARCH=${ARCH:-"i686"}
TARGET="$ARCH-pc-serenity"
PREFIX="$DIR/Local/$ARCH"
BUILD="$DIR/../Build/$ARCH"
SYSROOT="$BUILD/Root"

MAKE="make"
MD5SUM="md5sum"
NPROC="nproc"
REALPATH="realpath"

if command -v ginstall &>/dev/null; then
    INSTALL=ginstall
else
    INSTALL=install
fi

if [ "$(uname -s)" = "OpenBSD" ]; then
    MAKE=gmake
    MD5SUM="md5 -q"
    NPROC="sysctl -n hw.ncpuonline"
    REALPATH="readlink -f"
    export CC=egcc
    export CXX=eg++
    export with_gmp=/usr/local
    export LDFLAGS=-Wl,-z,notext
elif [ "$(uname -s)" = "FreeBSD" ]; then
    MAKE=gmake
    MD5SUM="md5 -q"
    NPROC="sysctl -n hw.ncpu"
    export with_gmp=/usr/local
    export with_mpfr=/usr/local
fi

# On at least OpenBSD, the path must exist to call realpath(3) on it
if [ ! -d "$BUILD" ]; then
    mkdir -p "$BUILD"
fi
BUILD=$($REALPATH "$BUILD")

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

BINUTILS_VERSION="2.36.1"
BINUTILS_MD5SUM="3df9c3bbd944f9b57c1496f06741197b"
BINUTILS_NAME="binutils-$BINUTILS_VERSION"
BINUTILS_PKG="${BINUTILS_NAME}.tar.gz"
BINUTILS_BASE_URL="http://ftp.gnu.org/gnu/binutils"

GCC_VERSION="10.3.0"
GCC_MD5SUM="87910940d70e845f2bf1a57997b6220c"
GCC_NAME="gcc-$GCC_VERSION"
GCC_PKG="${GCC_NAME}.tar.gz"
GCC_BASE_URL="http://ftp.gnu.org/gnu/gcc"

buildstep() {
    NAME=$1
    shift
    "$@" 2>&1 | sed 's|^|\x1b[34m['"${NAME}"']\x1b[39m |'
}

# === CHECK CACHE AND REUSE ===

pushd "$DIR"
    if [ "${TRY_USE_LOCAL_TOOLCHAIN}" = "y" ] ; then
        # The actual logic had to be moved to .github/workflows/cmake.yml.
        # Github Actions guarantees that Toolchain/Cache/ is empty on a cache
        # miss, and non-empty on a cache hit.
        # The following logic is correct *only* because of that.

        mkdir -p Cache
        echo "Cache (before):"
        ls -l Cache
        CACHED_TOOLCHAIN_ARCHIVE="Cache/ToolchainBinariesGithubActions.tar.gz"
        if [ -r "${CACHED_TOOLCHAIN_ARCHIVE}" ] ; then
            echo "Cache at ${CACHED_TOOLCHAIN_ARCHIVE} exists!"
            echo "Extracting toolchain from cache:"
            if tar xzf "${CACHED_TOOLCHAIN_ARCHIVE}" ; then
                echo "Done 'building' the toolchain."
                echo "Cache unchanged."
                exit 0
            else
                echo
                echo
                echo
                echo "Could not extract cached toolchain archive."
                echo "This means the cache is broken and *should be removed*!"
                echo "As Github Actions cannot update a cache, this will unnecessarily"
                echo "slow down all future builds for this hash, until someone"
                echo "resets the cache."
                echo
                echo
                echo
                rm -f "${CACHED_TOOLCHAIN_ARCHIVE}"
            fi
        else
            echo "Cache at ${CACHED_TOOLCHAIN_ARCHIVE} does not exist."
            echo "Will rebuild toolchain from scratch, and save the result."
        fi
        echo "::group::Actually building Toolchain"
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

    if [ -d ${BINUTILS_NAME} ]; then
        rm -rf "${BINUTILS_NAME}"
        rm -rf "$DIR/Build/$ARCH/$BINUTILS_NAME"
    fi
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
        $MD5SUM "$DIR"/Patches/binutils.patch > .patch.applied
    popd

    if [ -d ${GCC_NAME} ]; then
        # Drop the previously patched extracted dir
        rm -rf "${GCC_NAME}"
        # Also drop the build dir
        rm -rf "$DIR/Build/$ARCH/$GCC_NAME"
    fi
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
        $MD5SUM "$DIR/Patches/gcc.patch" > .patch.applied
    popd

    if [ "$(uname)" = "Darwin" ]; then
        pushd "gcc-${GCC_VERSION}"
        ./contrib/download_prerequisites
        popd
    fi
popd


# === COMPILE AND INSTALL ===

rm -rf "$PREFIX"
mkdir -p "$PREFIX"

if [ -z "$MAKEJOBS" ]; then
    MAKEJOBS=$($NPROC)
fi

mkdir -p "$DIR/Build/$ARCH"

pushd "$DIR/Build/$ARCH"
    unset PKG_CONFIG_LIBDIR # Just in case

    rm -rf binutils
    mkdir -p binutils

    pushd binutils
        echo "XXX configure binutils"
        buildstep "binutils/configure" "$DIR"/Tarballs/$BINUTILS_NAME/configure --prefix="$PREFIX" \
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
        buildstep "binutils/build" "$MAKE" -j "$MAKEJOBS" || exit 1
        buildstep "binutils/install" "$MAKE" install || exit 1
    popd

    echo "XXX serenity libc and libm headers"
    mkdir -p "$BUILD"
    pushd "$BUILD"
        mkdir -p Root/usr/include/
        SRC_ROOT=$($REALPATH "$DIR"/..)
        FILES=$(find "$SRC_ROOT"/Userland/Libraries/LibC "$SRC_ROOT"/Userland/Libraries/LibM -name '*.h' -print)
        for header in $FILES; do
            target=$(echo "$header" | sed -e "s@$SRC_ROOT/Userland/Libraries/LibC@@" -e "s@$SRC_ROOT/Userland/Libraries/LibM@@")
            buildstep "system_headers" $INSTALL -D "$header" "Root/usr/include/$target"
        done
        unset SRC_ROOT
    popd

    if [ "$(uname -s)" = "OpenBSD" ]; then
        perl -pi -e 's/-no-pie/-nopie/g' "$DIR/Tarballs/gcc-$GCC_VERSION/gcc/configure"
    fi

    if [ ! -f $DIR/Tarballs/gcc-$GCC_VERSION/gcc/config/serenity-userland.h ]; then
        cp $DIR/Tarballs/gcc-$GCC_VERSION/gcc/config/serenity.h $DIR/Tarballs/gcc-$GCC_VERSION/gcc/config/serenity-kernel.h
    fi

    for STAGE in Userland Kernel; do
        rm -rf gcc
        mkdir -p gcc

        pushd gcc
            TEMPTARGET="$BUILD/Temp"
            rm -rf "$TEMPTARGET"

            echo "XXX configure gcc and libgcc"
            if [ "$STAGE" = "Userland" ]; then
                REALTARGET="$PREFIX"
            else
                REALTARGET="$PREFIX/Kernel"
            fi

            cp $DIR/Tarballs/gcc-$GCC_VERSION/gcc/config/serenity-kernel.h $DIR/Tarballs/gcc-$GCC_VERSION/gcc/config/serenity.h
            if [ "$STAGE" = "Userland" ]; then
                sed -i 's@-fno-exceptions @@' $DIR/Tarballs/gcc-$GCC_VERSION/gcc/config/serenity.h
            fi

            buildstep "gcc/configure/${STAGE,,}" "$DIR/Tarballs/gcc-$GCC_VERSION/configure" --prefix="$PREFIX" \
                                                --target="$TARGET" \
                                                --with-sysroot="$SYSROOT" \
                                                --disable-nls \
                                                --with-newlib \
                                                --enable-shared \
                                                --enable-languages=c,c++ \
                                                --enable-default-pie \
                                                --enable-lto \
                                                ${TRY_USE_LOCAL_TOOLCHAIN:+"--quiet"} || exit 1

            if [ "$STAGE" = "Userland" ]; then
                echo "XXX build gcc and libgcc"
                buildstep "gcc/build" "$MAKE" -j "$MAKEJOBS" all-gcc || exit 1
                if [ "$(uname -s)" = "OpenBSD" ]; then
                    ln -sf liblto_plugin.so.0.0 gcc/liblto_plugin.so
                fi
                buildstep "libgcc/build" "$MAKE" -j "$MAKEJOBS" all-target-libgcc || exit 1
                echo "XXX install gcc and libgcc"
                buildstep "gcc+libgcc/install" "$MAKE" DESTDIR=$TEMPTARGET install-gcc install-target-libgcc || exit 1
            fi

            echo "XXX build libstdc++"
            buildstep "libstdc++/build/${STAGE,,}" "$MAKE" -j "$MAKEJOBS" all-target-libstdc++-v3 || exit 1
            echo "XXX install libstdc++"
            buildstep "libstdc++/install/${STAGE,,}" "$MAKE" DESTDIR=$TEMPTARGET install-target-libstdc++-v3 || exit 1

            mkdir -p "$REALTARGET"
            cp -a $TEMPTARGET/$PREFIX/* "$REALTARGET/"
            rm -rf "$TEMPTARGET"
        popd

        if [ "$STAGE" = "Userland" ]; then
            if [ "$(uname -s)" = "OpenBSD" ]; then
                cd "$DIR/Local/${ARCH}/libexec/gcc/$TARGET/$GCC_VERSION" && ln -sf liblto_plugin.so.0.0 liblto_plugin.so
            fi
        fi
    done
popd


# == STRIP BINARIES TO SAVE SPACE ==

pushd "$DIR"
    # Stripping doesn't seem to work on macOS.
    if [ "$(uname)" != "Darwin" ]; then
        # We *most definitely* don't need debug symbols in the linker/compiler.
        # This cuts the uncompressed size from 1.2 GiB per Toolchain down to about 120 MiB.
        # Hence, this might actually cause marginal speedups, although the point is to not waste space as blatantly.
        echo "Stripping executables ..."
        echo "Before: $(du -sh Local)"
        find Local/ -type f -executable ! -name '*.la' ! -name '*.sh' ! -name 'mk*' -exec strip {} +
        echo "After: $(du -sh Local)"
    fi
popd


# == SAVE TO CACHE ==

pushd "$DIR"
    if [ "${TRY_USE_LOCAL_TOOLCHAIN}" = "y" ] ; then
        echo "::endgroup::"
        echo "Building cache tar:"

        rm -f "${CACHED_TOOLCHAIN_ARCHIVE}"  # Just in case

        tar czf "${CACHED_TOOLCHAIN_ARCHIVE}" Local/

        echo "Cache (after):"
        ls -l Cache
    fi
popd
