#!/usr/bin/env bash
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# shellcheck source=/dev/null
. "${DIR}/../Meta/shell_include.sh"

exit_if_running_as_root "Do not run BuildGDB.sh as root, parts of your Toolchain directory will become root-owned"

GDB_VERSION="13.1"
GDB_MD5SUM="4aaad768ff2585464173c091947287ec"
GDB_NAME="gdb-$GDB_VERSION"
GDB_PKG="${GDB_NAME}.tar.xz"
GDB_BASE_URL="https://ftpmirror.gnu.org/gnu/gdb"

ARCH=${1:-"x86_64"}
TARGET="$ARCH-pc-serenity"
PREFIX="$DIR/Local/$ARCH-gdb"

echo "Building GDB $GDB_VERSION for $TARGET"

MD5SUM="md5sum"

SYSTEM_NAME="$(uname -s)"

if [ "$SYSTEM_NAME" = "OpenBSD" ]; then
    MD5SUM="md5 -q"
    export CC=egcc
    export CXX=eg++
    export with_gmp=/usr/local
    export LDFLAGS=-Wl,-z,notext
elif [ "$SYSTEM_NAME" = "FreeBSD" ]; then
    MD5SUM="md5 -q"
    export with_gmp=/usr/local
    export with_mpfr=/usr/local
elif [ "$SYSTEM_NAME" = "Darwin" ]; then
    MD5SUM="md5 -q"
fi

NPROC=$(get_number_of_processing_units)
[ -z "$MAKEJOBS" ] && MAKEJOBS=${NPROC}

buildstep() {
    NAME=$1
    shift
    "$@" 2>&1 | sed $'s|^|\x1b[34m['"${NAME}"$']\x1b[39m |'
}

missing_lib() {
    buildstep dependencies echo "Please make sure to install the $lib library and headers." >&2
    exit 1
}

config_args=()
for lib in gmp isl mpfr mpc; do
    buildstep dependencies echo "Checking whether the $lib library and headers are available..."
    if [ "$SYSTEM_NAME" = "Darwin" ]; then
        [ "$lib" = "mpc" ] && formula_name="libmpc" || formula_name="$lib"
        config_args+=("--with-$lib=$(brew --prefix --installed "$formula_name")") || missing_lib $lib
    else
        [ "$lib" = "isl" ] && header="isl/version.h" || header="$lib.h"
        if ! ${CC:-cc} -I /usr/local/include -L /usr/local/lib -l$lib -o /dev/null -xc - >/dev/null <<PROGRAM
#include <$header>
int main() {}
PROGRAM
              then
                  missing_lib $lib
              fi
    fi
done

if [ "$SYSTEM_NAME" = "Darwin" ]; then
    config_args+=("--with-libgmp-prefix=$(brew --prefix gmp)")
fi

mkdir -p "$DIR/Tarballs"

pushd "$DIR/Tarballs"
    md5=""
    if [ -e "$GDB_PKG" ]; then
        md5="$($MD5SUM "$GDB_PKG" | cut -f1 -d' ')"
    fi
    if [ "$md5" != "$GDB_MD5SUM" ]; then
        curl -C - -LO "$GDB_BASE_URL/$GDB_PKG"
    else
        echo "Skipped downloading $GDB_PKG"
    fi

    md5="$($MD5SUM "$GDB_PKG" | cut -f1 -d' ')"
    echo "gdb md5='$md5'"
    if  [ "$md5" != "$GDB_MD5SUM" ] ; then
        echo "gdb md5 sum mismatching, please run script again."
        rm -f "$GDB_PKG"
        exit 1
    fi

    # If the source directory exists, re-extract it again in case the patches have changed.
    if [ -d ${GDB_NAME} ]; then
        rm -rf "${GDB_NAME}"
        rm -rf "$DIR/Build/$ARCH-gdb"
    fi
    echo "Extracting gdb..."
    tar -xJf "$GDB_PKG"

    pushd "$GDB_NAME"
        for patch in "${DIR}"/Patches/gdb/*.patch; do
            patch -p1 < "${patch}" > /dev/null
        done
    popd
popd

mkdir -p "$DIR/Build"

rm -rf "$DIR/Build/$ARCH-gdb"
mkdir "$DIR/Build/$ARCH-gdb"

pushd "$DIR/Build/$ARCH-gdb"
    unset PKG_CONFIG_LIBDIR # Just in case

    buildstep "gdb/configure" "$DIR"/Tarballs/$GDB_NAME/configure --prefix="$PREFIX" \
                                                                  --target="$TARGET" \
                                                                  --disable-werror \
                                                                  --disable-nls \
                                                                  --with-python \
                                                                  "${config_args[@]}" || exit 1

    buildstep "gdb/build" make MAKEINFO=true -j "$MAKEJOBS" || exit 1
    buildstep "gdb/install" make MAKEINFO=true install || exit 1
popd
