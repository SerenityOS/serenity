#!/usr/bin/env -S bash ../.port_include.sh
port='flac'
version='1.4.3'
useconfigure='true'
depends=(
    'libogg'
)
files=(
    "https://downloads.xiph.org/releases/flac/flac-${version}.tar.xz#6c58e69cd22348f441b861092b825e591d0b822e106de6eb0ee4d05d27205b70"
)
