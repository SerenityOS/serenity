#!/bin/bash

set -e

die() {
    echo "die: $@"
    exit 1
}

if [ $(id -u) != 0 ]; then
    die "this script needs to run as root"
fi

echo "setting up disk image..."
qemu-img create _disk_image ${DISK_SIZE:-500}m || die "couldn't create disk image"
chown $build_user:$build_group _disk_image || die "couldn't adjust permissions on disk image"
echo "done"

echo -n "creating new filesystem... "
mke2fs -q -I 128 _disk_image || die "couldn't create filesystem"
echo "done"

echo -n "mounting filesystem... "
mkdir -p mnt
mount _disk_image mnt/ || die "couldn't mount filesystem"
echo "done"

cleanup() {
    if [ -d mnt ]; then
        echo -n "unmounting filesystem... "
        umount mnt || ( sleep 1 && sync && umount mnt )
        rm -rf mnt
        echo "done"
    fi
}
trap cleanup EXIT

./build-root-filesystem.sh
