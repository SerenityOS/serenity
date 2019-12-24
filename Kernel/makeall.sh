#!/bin/sh
set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "$script_path"

# Get user and group details for setting qemu disk image ownership
build_user=$(id -u)
build_group=$(id -g)
export build_user
export build_group

sudo id

make -C ../ clean && \
    make -C ../ && \
    make -C ../ test && \
    make -C ../ install &&
    sudo -E PATH="$PATH" ./build-image-qemu.sh
