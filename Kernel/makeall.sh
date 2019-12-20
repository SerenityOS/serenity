#!/bin/sh
if [ -z "$SERENITY_ROOT" ]; then
    echo "You must source Toolchain/UseIt.sh to build Serenity."
    exit 1
fi

set -e

# Get user and group details for setting qemu disk image ownership
export build_user=$(id -u)
export build_group=$(id -g)

sudo id

make -C ../ clean && \
    make -C ../ && \
    make -C ../ test && \
    make -C ../ install &&
    sudo -E PATH="$PATH" ./build-image-qemu.sh
