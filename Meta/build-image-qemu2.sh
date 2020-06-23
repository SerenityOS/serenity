#!/bin/sh

set -e

die() {
    echo "die: $*"
    exit 1
}

echo "setting up disk image..."
qemu-img create _disk_image "${DISK_SIZE:-600}"m || die "could not create disk image"
echo "done"

cleanup() {
    if [ -d mnt ]; then
        printf "unmounting filesystem... "
        rm -rf mnt
        echo "done"
    fi
}
trap cleanup EXIT

mkdir mnt
script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
"$script_path/build-root-filesystem.sh"

printf "creating new filesystem... "
mke2fs -q -I 128 -d mnt _disk_image || die "could not create filesystem"
echo "done"

