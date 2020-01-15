#!/bin/sh

set -e

die() {
    echo "die: $*"
    exit 1
}

if [ "$(id -u)" != 0 ]; then
    die "this script needs to run as root"
fi
if [ "$(uname -s)" = "Darwin" ]; then
    export PATH="/usr/local/opt/e2fsprogs/bin:$PATH"
    export PATH="/usr/local/opt/e2fsprogs/sbin:$PATH"
fi
echo "setting up disk image..."
qemu-img create _disk_image "${DISK_SIZE:-600}"m || die "couldn't create disk image"
chown "$SUDO_UID":"$SUDO_GID" _disk_image || die "couldn't adjust permissions on disk image"
echo "done"

printf "creating new filesystem... "
if [ "$(uname -s)" = "OpenBSD" ]; then
    VND=`vnconfig _disk_image`
    (echo "e 0"; echo 83; echo n; echo 0; echo "*"; echo "quit") | fdisk -e $VND
    mkfs.ext2 -I 128 -F /dev/${VND}i || die "couldn't create filesystem"
else
    mke2fs -q -I 128 _disk_image || die "couldn't create filesystem"
fi
echo "done"

printf "mounting filesystem... "
mkdir -p mnt
if [ "$(uname -s)" = "Darwin" ]; then
    fuse-ext2 _disk_image mnt -o rw+,allow_other,uid=501,gid=20 || die "couldn't mount filesystem"
elif [ "$(uname -s)" = "OpenBSD" ]; then
    mount -t ext2fs /dev/${VND}i mnt/ || die "couldn't mount filesystem"
else
    mount _disk_image mnt/ || die "couldn't mount filesystem"
fi
echo "done"

cleanup() {
    if [ -d mnt ]; then
        printf "unmounting filesystem... "
        umount mnt || ( sleep 1 && sync && umount mnt )
        rm -rf mnt
        if [ "$(uname -s)" = "OpenBSD" ]; then
           vnconfig -u $VND
        fi
        echo "done"
    fi
}
trap cleanup EXIT

./build-root-filesystem.sh
