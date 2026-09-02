#!/usr/bin/env -S bash ../.port_include.sh
port='patchelf'
version='0.19.1'
useconfigure='true'
files=(
    "https://github.com/NixOS/patchelf/releases/download/${version}/patchelf-${version}.tar.gz#491108728f120ce05b539934b41a750235031a6df8abc6b47e57aff7de15094d"
)
