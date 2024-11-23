#!/usr/bin/env bash

set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

. "${script_path}/shell_include.sh"

if [ ! -d "limine" ]; then
    echo "limine not found, the script will now build it"
    git clone --depth 1 --branch v2.78.2 --single-branch https://github.com/limine-bootloader/limine
    cd limine
    ./autogen.sh
    make all
    cd ..
fi

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

DISK_SIZE=$(($(disk_usage "$SERENITY_SOURCE_DIR/Base") + $(disk_usage Root) + 300))

echo "setting up disk image..."
dd if=/dev/zero of=limine_disk_image bs=1M count="${DISK_SIZE:-800}" status=none || die "couldn't create disk image"
chown "$SUDO_UID":"$SUDO_GID" limine_disk_image || die "couldn't adjust permissions on disk image"
echo "done"

printf "creating loopback device... "
dev=$(losetup --find --partscan --show limine_disk_image)
if [ -z "$dev" ]; then
    die "couldn't mount loopback device"
fi
echo "loopback device is at ${dev}"

cleanup() {
    if [ -d mnt ]; then
        printf "unmounting root partition... "
        umount -R mnt || ( sleep 1 && sync && umount -R mnt )
        rmdir mnt
        echo "done"
    fi

    if [ -d esp ]; then
        printf "unmounting efi partition... "
        umount -R esp || ( sleep 1 && sync && umount -R esp )
        rmdir esp
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
parted -s "${dev}" mklabel gpt mkpart EFI fat32 1MiB 10MiB mkpart ROOT ext2 10MiB 100% set 1 esp on || die "couldn't partition disk"
echo "done"

printf "creating new filesystems... "
mkfs.vfat -F 32 "${dev}p1" || die "couldn't create efi filesystem"
mke2fs -q "${dev}p2" || die "couldn't create root filesystem"
echo "done"

printf "mounting filesystems... "
mkdir -p esp
mount "${dev}p1" esp || die "couldn't mount efi filesystem"
mkdir -p mnt
mount "${dev}p2" mnt || die "couldn't mount root filesystem"
echo "done"

"$script_path/build-root-filesystem.sh"

echo "installing limine"
mkdir -p esp/EFI/BOOT
cp limine/bin/limine.sys esp
cp limine/bin/BOOTX64.EFI esp/EFI/BOOT
cp "$SERENITY_SOURCE_DIR"/Meta/limine.cfg esp
limine/bin/limine-install "${dev}"
echo "done"
