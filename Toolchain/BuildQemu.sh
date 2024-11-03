#!/usr/bin/env bash
set -e

# This file will need to be run in bash, for now.

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# shellcheck source=/dev/null
. "${DIR}/../Meta/shell_include.sh"

exit_if_running_as_root "Do not run BuildQemu.sh as root, parts of your Toolchain directory will become root-owned"

echo "$DIR"

PREFIX="$DIR/Local/qemu"
BUILD=$(realpath "$DIR/../Build")
SYSROOT="$BUILD/Root"

# shellcheck source=/dev/null
source "${DIR}/../Ports/qemu/version.sh"

echo PREFIX is "$PREFIX"
echo SYSROOT is "$SYSROOT"

mkdir -p "$DIR/Tarballs"

pushd "$DIR/Tarballs"
    if [ ! -e "${QEMU_ARCHIVE}" ]; then
        curl -C - -O "${QEMU_ARCHIVE_URL}"
    else
        echo "Skipped downloading ${QEMU_ARCHIVE}"
    fi

    if ! check_sha256 "${QEMU_ARCHIVE}" "${QEMU_ARCHIVE_SHA256SUM}"; then
        echo "qemu sha256 sum mismatching, please run script again."
        rm -f "${QEMU_ARCHIVE}"
        exit 1
    fi

    if [ -d "qemu-$QEMU_VERSION" ]; then
        rm -rf "qemu-$QEMU_VERSION"
    fi

    echo "Extracting qemu..."
    tar -xf "${QEMU_ARCHIVE}"
popd

mkdir -p "$PREFIX"
mkdir -p "$DIR/Build/qemu"

NPROC=$(get_number_of_processing_units)
[ -z "$MAKEJOBS" ] && MAKEJOBS=${NPROC}

EXTRA_ARGS=""
if [[ $(uname) == "Darwin" ]]
then
    UI_LIB=cocoa

    # SDL causes a crash on startup: "NSWindow drag regions should only be invalidated on the Main Thread!"
    EXTRA_ARGS="--disable-sdl"
else
    UI_LIB=gtk
    # Allows copy pasting between Serenity and the host.
    EXTRA_ARGS="--enable-gtk-clipboard"
fi

echo Using $UI_LIB based UI

pushd "$DIR/Build/qemu"
    "$DIR"/Tarballs/qemu-"${QEMU_VERSION}"/configure --prefix="$PREFIX" \
                                            --target-list=aarch64-softmmu,x86_64-softmmu,riscv64-softmmu \
                                            --enable-$UI_LIB \
                                            --enable-slirp \
                                            $EXTRA_ARGS || exit 1
    make -j "$MAKEJOBS" || exit 1
    make install || exit 1
popd
