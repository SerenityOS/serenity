#!/bin/sh
set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

sudo -E PATH="$PATH" "$script_path/build-image-qemu.sh"
