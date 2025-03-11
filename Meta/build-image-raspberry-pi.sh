#!/usr/bin/env bash

set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

. "${script_path}/shell_include.sh"

DISK_SIZE=$(($(disk_usage "$SERENITY_SOURCE_DIR/Base") + $(disk_usage Root) + 512 + 300))

if [ "$1" != "in-sudo" ]; then # Avoid making the repo root-owned when recursively running this script with sudo.
    if [ ! -d "raspberry-pi-firmware" ]; then
        echo "raspberry-pi-firmware directory not found, cloning the boot/ directory from https://github.com/raspberrypi/firmware"

        # Do a sparse checkout of only the boot/ directory, since the repo is pretty large.
        git clone --depth 1 --filter tree:0 --branch stable --no-checkout https://github.com/raspberrypi/firmware raspberry-pi-firmware
        pushd raspberry-pi-firmware >/dev/null
        git sparse-checkout set boot/
        git checkout
        popd >/dev/null
    else
        pushd raspberry-pi-firmware >/dev/null
        git pull --ff-only
        popd >/dev/null
    fi
fi

# Remove unnecessary linux kernel images.
rm -f raspberry-pi-firmware/boot/kernel*.img

if [ "$(id -u)" != 0 ]; then
    set +e
    ${SUDO} -- "${SHELL}" -c "\"$0\" in-sudo || exit 42"
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

printf "setting up disk image... "
dd if=/dev/zero of=raspberry_pi_disk_image bs=1M count="${DISK_SIZE}" status=none || die "couldn't create disk image"
chown "$SUDO_UID":"$SUDO_GID" raspberry_pi_disk_image || die "couldn't adjust permissions on disk image"
echo "done"

printf "creating loopback device... "
dev=$(losetup --find --partscan --show raspberry_pi_disk_image)
if [ -z "$dev" ]; then
    die "couldn't mount loopback device"
fi
echo "loopback device is at ${dev}"

cleanup() {
    if [ -d boot ]; then
        printf "unmounting boot filesystem... "
        umount boot || ( sleep 1 && sync && umount boot )
        rmdir boot
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
parted -s "${dev}" mklabel msdos mkpart primary fat32 0% 512MB mkpart primary ext2 512MB 100% || die "couldn't partition disk"
echo "done"

printf "creating new filesystems... "
mkfs.vfat -F 32 -n BOOT "${dev}p1" >/dev/null || die "couldn't create boot filesystem"
mke2fs -q -L SerenityOS "${dev}p2" || die "couldn't create root filesystem"
echo "done"

printf "mounting filesystems... "
mkdir -p mnt
mount "${dev}p2" mnt || die "couldn't mount root filesystem"
mkdir -p boot
mount "${dev}p1" boot || die "couldn't mount boot filesystem"
echo "done"

"$script_path/build-root-filesystem.sh"

cp -r raspberry-pi-firmware/boot/* boot/

cat <<EOF >boot/config.txt
# We only support AArch64.
arm_64bit=1

# Enable the bootloader debug log.
uart_2ndstage=1
enable_uart=1
enable_gic=1

kernel=Kernel.bin

[pi4]
# We use UART0 as the console.
dtoverlay=disable-bt
EOF

echo "serial_debug root=block100:1" >boot/cmdline.txt

# FIXME: Mount the boot partition on /boot (both here and in serenity), so we don't need to move the kernel image to the boot filesystem.
mv mnt/boot/Kernel.bin boot/
