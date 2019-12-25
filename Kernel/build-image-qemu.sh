#!/bin/sh

set -e

die() {
    echo "die: $*"
    exit 1
}

if [ "$(id -u)" != 0 ]; then
    die "this script needs to run as root"
fi
if [ "$(uname)" = "Darwin" ]; then
    export PATH="/usr/local/opt/e2fsprogs/bin:$PATH"
    export PATH="/usr/local/opt/e2fsprogs/sbin:$PATH"
fi
echo "setting up disk image..."
qemu-img create _disk_image "${DISK_SIZE:-600}"m || die "couldn't create disk image"
chown "$build_user":"$build_group" _disk_image || die "couldn't adjust permissions on disk image"
echo "done"

printf "creating new filesystem... "
mke2fs -q -I 128 _disk_image || die "couldn't create filesystem"
echo "done"

printf "mounting filesystem... "
mkdir -p mnt
if [ "$(uname)" = "Darwin" ]; then
    fuse-ext2 _disk_image mnt -o rw+,allow_other,uid=501,gid=20 || die "couldn't mount filesystem"
else
    mount _disk_image mnt/ || die "couldn't mount filesystem"
fi
echo "done"

cleanup() {
    if [ -d mnt ]; then
        printf "unmounting filesystem... "
        umount mnt || ( sleep 1 && sync && umount mnt )
        rm -rf mnt
        echo "done"
    fi
}
trap cleanup EXIT

./build-root-filesystem.sh
