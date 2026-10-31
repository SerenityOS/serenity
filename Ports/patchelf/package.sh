#!/usr/bin/env -S bash ../.port_include.sh
port='patchelf'
version='0.19.3'
useconfigure='true'
files=(
    "https://github.com/NixOS/patchelf/releases/download/${version}/patchelf-${version}.tar.gz#fac76a3c0b730e8d53ea1d10cd6015322d78c9900af91736f2cca51d5837b612"
)
