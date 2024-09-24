#!/usr/bin/env -S bash ../.port_include.sh
port='patchelf'
version='0.18.0'
useconfigure='true'
files=(
    "https://github.com/NixOS/patchelf/releases/download/${version}/patchelf-${version}.tar.gz#64de10e4c6b8b8379db7e87f58030f336ea747c0515f381132e810dbf84a86e7"
)
