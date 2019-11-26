#!/bin/sh

set -e

die() {
    echo "die: $*"
    exit 1
}

if [ "$(id -u)" != 0 ]; then
    die "this script needs to run as root"
fi

grub=$(command -v grub-install 2>/dev/null) || true
if [ -z "$grub" ]; then
    grub=$(command -v grub2-install 2>/dev/null) || true
fi
if [ -z "$grub" ]; then
    echo "can't find a grub-install or grub2-install binary, oh no"
    exit 1
fi
echo "using grub-install at ${grub}"

echo "setting up disk image..."
dd if=/dev/zero of=_disk_image bs=1M count="${DISK_SIZE:-600}" status=none || die "couldn't create disk image"
chown 1000:1000 _disk_image || die "couldn't adjust permissions on disk image"
echo "done"

printf "creating loopback device... "
dev=$(losetup --find --partscan --show _disk_image)
if [ -z "$dev" ]; then
    die "couldn't mount loopback device"
fi
echo "loopback device is at ${dev}"

cleanup() {
    if [ -d mnt ]; then
        printf "unmounting filesystem... "
        umount mnt || ( sleep 1 && sync && umount mnt )
        rm -rf mnt
        echo "done"
    fi

    if [ -e "${dev}" ]; then
        printf "cleaning up loopback device... "
        losetup -d "${dev}"
        echo "done"
    fi
}
trap cleanup EXIT

printf "creating partition table... "
parted -s "${dev}" mklabel msdos mkpart primary ext2 32k 100% -a minimal set 1 boot on || die "couldn't partition disk"
echo "done"

printf "destroying old filesystem... "
dd if=/dev/zero of="${dev}"p1 bs=1M count=1 status=none || die "couldn't destroy old filesystem"
echo "done"

printf "creating new filesystem... "
mke2fs -q -I 128 "${dev}"p1 || die "couldn't create filesystem"
echo "done"

printf "mounting filesystem... "
mkdir -p mnt
mount "${dev}"p1 mnt/ || die "couldn't mount filesystem"
echo "done"

./build-root-filesystem.sh

printf "creating /boot... "
mkdir -p mnt/boot
echo "done"

echo "installing grub using $grub..."
$grub --boot-directory=mnt/boot --target=i386-pc --modules="ext2 part_msdos" "${dev}"

if [ -d mnt/boot/grub2 ]; then
    cp grub.cfg mnt/boot/grub2/grub.cfg
else
    cp grub.cfg mnt/boot/grub/grub.cfg
fi
echo "done"

printf "installing kernel in /boot... "
cp kernel mnt/boot
echo "done"
