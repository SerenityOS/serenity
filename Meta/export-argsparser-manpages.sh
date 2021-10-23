#!/bin/sh

set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "${script_path}/.."

if ! command -v tar >/dev/null 2>&1 ; then
    echo "Please install tar!"
    exit 1
fi

echo "This script assumes passwordless sudo."
sudo true

if [ -z "$BUILD_DIR" ]; then
    if [ -z "$SERENITY_ARCH" ]; then
        export SERENITY_ARCH="i686"
        echo "SERENITY_ARCH not given. Assuming ${SERENITY_ARCH}."
    fi
    BUILD_DIR=Build/"$SERENITY_ARCH"
    echo "BUILD_DIR not given. Assuming ${BUILD_DIR}."
fi

if [ -e fsmount ]; then
    echo "Directory 'fsmount' already exists."
    echo "Manual cleanup needed. You might also need to unmount first."
    exit 1
fi

if ! [ -d Base/usr/share/man/ ]; then
    echo "Base/usr/share/man/ does not exist. How did that happen?! o.O"
    exit 1
fi

echo "Using 'ninja run' to generate manpages ..."
export SERENITY_RUN="ci"
export SERENITY_KERNEL_CMDLINE="fbdev=off panic=shutdown system_mode=generate-manpages"
# The 'sed' gets rid of the clear-screen escape sequence.
ninja -C "$BUILD_DIR" -- run | sed -re 's,''c,,'
echo

echo "Extracting generated manpages ..."
mkdir fsmount
sudo mount -t ext2 -o loop,rw "$BUILD_DIR"/_disk_image fsmount
# 'cp' would create the new files as root, but we don't want that.
sudo tar -C fsmount/root/generated_manpages --create . | tar -C Base/usr/share/man/ --extract
sudo umount fsmount
rmdir fsmount

echo "Successfully (re-)generated manpages in Base/usr/share/man/"
