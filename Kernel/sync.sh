#!/bin/sh
set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "$script_path"

if command -v genext2fs 1>/dev/null && command -v fakeroot 1>/dev/null; then
    fakeroot ./build-image-qemu.sh
else
    sudo -E PATH="$PATH" ./build-image-qemu.sh
fi
