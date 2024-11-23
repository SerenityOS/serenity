#!/usr/bin/env bash

set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

. "${script_path}/shell_include.sh"

if [ "$(id -u)" != 0 ]; then
    set +e
    ${SUDO} -- "${SHELL}" -c "\"$0\" $* || exit 42"
    case $? in
        1)
            die "this script needs to run as root"
            ;;
        42)
            exit 1
            ;;
        *)
            exit 0
            ;;
    esac
else
    : "${SUDO_UID:=0}" "${SUDO_GID:=0}"
fi

for dir in "/usr/lib/syslinux/bios" "/usr/lib/syslinux" "/usr/share/syslinux"; do
    if [ -d $dir ]; then
        syslinux_dir=$dir
        break
    fi
done
if [ -z $syslinux_dir ]; then
    echo "can't find syslinux directory"
    exit 1
fi

DISK_SIZE=$(($(disk_usage "$SERENITY_SOURCE_DIR/Base") + $(disk_usage Root) + 300))

echo "setting up disk image..."
dd if=/dev/zero of=extlinux_disk_image bs=1M count="${DISK_SIZE:-800}" status=none || die "couldn't create disk image"
chown "$SUDO_UID":"$SUDO_GID" extlinux_disk_image || die "couldn't adjust permissions on disk image"
echo "done"

printf "creating loopback device... "
dev=$(losetup --find --partscan --show extlinux_disk_image)
if [ -z "$dev" ]; then
    die "couldn't mount loopback device"
fi
echo "loopback device is at ${dev}"

cleanup() {
    if [ -d mnt ]; then
        printf "unmounting filesystem... "
        umount mnt || ( sleep 1 && sync && umount mnt )
        rmdir mnt
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
partition_number="p1"
echo "done"

printf "destroying old filesystem... "
dd if=/dev/zero of="${dev}${partition_number}" bs=1M count=1 status=none || die "couldn't destroy old filesystem"
echo "done"

printf "creating new filesystem... "
mke2fs -q "${dev}${partition_number}" || die "couldn't create filesystem"
echo "done"

printf "mounting filesystem... "
mkdir -p mnt
mount "${dev}${partition_number}" mnt/ || die "couldn't mount filesystem"
echo "done"

"$script_path/build-root-filesystem.sh"

if [ -z "$1" ]; then
    extlinux_cfg="$SERENITY_SOURCE_DIR"/Meta/extlinux.conf
else
    extlinux_cfg=$1
fi

echo "installing extlinux with $extlinux_cfg..."
mkdir -p mnt/boot/extlinux
extlinux --install mnt/boot/extlinux
cp "$extlinux_cfg" mnt/boot/extlinux/extlinux.conf
for module in mboot.c32 menu.c32 libutil.c32 libcom32.c32; do
    cp $syslinux_dir/$module mnt/boot/extlinux/
done
dd bs=440 count=1 conv=notrunc if=$syslinux_dir/mbr.bin of="$dev"
echo "done"
