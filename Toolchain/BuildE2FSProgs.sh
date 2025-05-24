#!/usr/bin/env bash

set -eu

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

. "${DIR}/../Meta/shell_include.sh"

exit_if_running_as_root "Do not run BuildE2FSProgs.sh as root, parts of your Toolchain directory will become root-owned"

NPROC=$(get_number_of_processing_units)
[ -z "${MAKEJOBS-}" ] && MAKEJOBS=${NPROC}

# Once b022aca269a5552395393989a4be530cbf7b5e70 is in a released version, use a tarball from https://www.kernel.org/pub/linux/kernel/people/tytso/e2fsprogs/.
GIT_REPO=https://git.kernel.org/pub/scm/fs/ext2/e2fsprogs.git
GIT_REV=241dae1b68aabe121974d095c150e7d2f9f33ade

SOURCE_DIR="$DIR/Tarballs/e2fsprogs"
BUILD_DIR="$DIR"/Build/e2fsprogs
PREFIX="$DIR/Local/e2fsprogs"

mkdir -p "$DIR"/Tarballs
pushd "$DIR"/Tarballs
    [ ! -d e2fsprogs ] && git clone $GIT_REPO

    cd e2fsprogs
    git fetch origin
    git checkout $GIT_REV
popd

mkdir -p "$PREFIX"
mkdir -p "$BUILD_DIR"

pushd "$BUILD_DIR"
    "$SOURCE_DIR"/configure --prefix="$PREFIX" --with-udev-rules-dir=no --with-crond-dir=no --with-systemd-unit-dir=no --sbindir="$PREFIX"/bin
    make -j "$MAKEJOBS"
    make install
popd
