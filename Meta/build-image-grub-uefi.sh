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

grub=$(command -v grub-install 2>/dev/null) || true
if [ -z "$grub" ]; then
    grub=$(command -v grub2-install 2>/dev/null) || true
fi
if [ -z "$grub" ]; then
    echo "can't find a grub-install or grub2-install binary"
    exit 1
fi
echo "using grub-install at ${grub}"

DISK_SIZE=$(($(disk_usage "$SERENITY_SOURCE_DIR/Base") + $(disk_usage Root) + 512 + 300))

printf "setting up disk image... "
dd if=/dev/zero of=grub_uefi_disk_image bs=1M count="${DISK_SIZE}" status=none || die "couldn't create disk image"
chown "$SUDO_UID":"$SUDO_GID" grub_uefi_disk_image || die "couldn't adjust permissions on disk image"
echo "done"

printf "creating loopback device... "
dev=$(losetup --find --partscan --show grub_uefi_disk_image)
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

echo "installing grub using $grub..."
$grub --target=x86_64-efi --efi-directory=esp/ --boot-directory=esp/ --removable
echo "done"

esp_uuid=$(blkid -o export "${dev}p1" | grep ^UUID | cut -d= -f2)
root_uuid=$(blkid -o export "${dev}p2" | grep ^PARTUUID | cut -d= -f2)

cat <<EOF >esp/grub/grub.cfg
timeout=1

search --no-floppy --fs-uuid --set=root ${esp_uuid}

menuentry 'SerenityOS (normal)' {
  chainloader /Kernel.efi root=PARTUUID:${root_uuid}
}

menuentry 'SerenityOS (text mode)' {
  chainloader /Kernel.efi graphics_subsystem_mode=off root=PARTUUID:${root_uuid}
}

menuentry 'SerenityOS (No ACPI)' {
  chainloader /Kernel.efi root=PARTUUID:${root_uuid} acpi=off
}

menuentry 'SerenityOS (with serial debug)' {
  chainloader /Kernel.efi serial_debug root=PARTUUID:${root_uuid}
}
EOF

# FIXME: Mount the EFI system partition on /boot (both here and in serenity), so we don't need to move the Kernel.efi file to the ESP.
mv mnt/boot/Kernel.efi esp/
