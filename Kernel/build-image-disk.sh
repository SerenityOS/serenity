#!/bin/bash

set -e

die() {
    echo "die: $@"
    exit 1
}

if [ $(id -u) != 0 ]; then
    die "this script needs to run as root"
fi

if [ -z $BOOT_DEVICE ]; then
    die "must define BOOT_DEVICE"
fi

if [ -z $ROOT_DEVICE ]; then
    die "must define ROOT_DEVICE"
fi

cleanup() {
    if [ -d mnt ]; then
        if [ -d mnt/boot ]; then
            echo -n "unmounting boot filesystem... "
            umount mnt/boot || ( sleep 1 && sync && umount mnt/boot ) || die "couldn't unmount boot filesystem"
            echo "done"
        fi

        echo -n "unmounting root filesystem... "
        umount mnt || ( sleep 1 && sync && umount mnt ) || die "couldn't unmount root filesystem"
        rm -rf mnt
        echo "done"
    fi
}
trap cleanup EXIT

echo -n "destroying old filesystem... "
dd if=/dev/zero of=${ROOT_DEVICE} bs=1M count=1 status=none || die "couldn't destroy old filesystem"
echo "done"

echo -n "creating new filesystem... "
mke2fs -q -I 128 ${ROOT_DEVICE} || die "couldn't create filesystem"
echo "done"

echo -n "mounting root filesystem... "
mkdir -p mnt
mount ${ROOT_DEVICE} mnt || die "couldn't mount root filesystem"
echo "done"

echo -n "mounting boot filesystem... "
mkdir -p mnt/boot
mount ${BOOT_DEVICE} mnt/boot || die "couldn't mount boot filesystem"
echo "done"

./build-root-filesystem.sh

echo -n "installing kernel in /boot... "
cp kernel mnt/boot/serenity
echo "done"
