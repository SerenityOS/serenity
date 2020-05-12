#!/bin/sh
set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "$script_path"

sudo -E PATH="$PATH" ./build-image-qemu.sh
