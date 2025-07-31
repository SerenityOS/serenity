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

DISK_SIZE=$(($(disk_usage "$SERENITY_SOURCE_DIR/Base") + $(disk_usage Root) + 512 + 300))

printf "setting up disk image... "
dd if=/dev/zero of=uefi_disk_image bs=1M count="${DISK_SIZE}" status=none || die "couldn't create disk image"
chown "$SUDO_UID":"$SUDO_GID" uefi_disk_image || die "couldn't adjust permissions on disk image"
echo "done"

printf "creating loopback device... "
dev=$(losetup --find --partscan --show uefi_disk_image)
if [ -z "$dev" ]; then
    die "couldn't mount loopback device"
fi
echo "loopback device is at ${dev}"

cleanup() {
    if [ -d esp ]; then
        printf "unmounting EFI system partition... "
        umount esp || ( sleep 1 && sync && umount esp )
        rmdir esp
        echo "done"
    fi

    if [ -d mnt ]; then
        printf "unmounting root filesystem... "
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
parted -s "${dev}" mklabel gpt mkpart EFI fat32 0% 512MB mkpart SerenityOS ext2 512MB 100% set 1 esp on || die "couldn't partition disk"
echo "done"

printf "creating new filesystems... "
mkfs.vfat -F 32 -n EFI "${dev}p1" >/dev/null || die "couldn't create EFI system partition filesystem"
mke2fs -q -L SerenityOS "${dev}p2" || die "couldn't create root filesystem"
echo "done"

printf "mounting filesystems... "
mkdir -p esp
mount "${dev}p1" esp || die "couldn't mount EFI system partition"
mkdir -p mnt
mount "${dev}p2" mnt || die "couldn't mount root filesystem"
echo "done"

"$script_path/build-root-filesystem.sh"

case $SERENITY_ARCH in
    aarch64)
        BOOT_FILE_NAME=BOOTAA64.EFI
        ;;
    riscv64)
        BOOT_FILE_NAME=BOOTRISCV64.EFI
        ;;
    x86_64)
        BOOT_FILE_NAME=BOOTX64.EFI
        ;;
    *)
        die "unknown architecture: $SERENITY_ARCH"
        ;;
esac

root_uuid=$(blkid -o export "${dev}p2" | grep ^PARTUUID | cut -d= -f2)

cat <<EOF >esp/cmdline.txt
root=PARTUUID:${root_uuid}
EOF

mkdir -p esp/EFI/BOOT
cp mnt/boot/Kernel.efi esp/EFI/BOOT/$BOOT_FILE_NAME
