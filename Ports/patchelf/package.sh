#!/usr/bin/env -S bash ../.port_include.sh
port='patchelf'
version='0.19.2'
useconfigure='true'
files=(
    "https://github.com/NixOS/patchelf/releases/download/${version}/patchelf-${version}.tar.gz#78cb26bda2b89fababfe189c2f6507240340f1aa45dec2eb49a39473a50ceea1"
)
