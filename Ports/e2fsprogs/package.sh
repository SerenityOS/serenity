#!/usr/bin/env -S bash ../.port_include.sh
port='e2fsprogs'
version='1.47.4'
files=(
    "https://www.kernel.org/pub/linux/kernel/people/tytso/e2fsprogs/v${version}/e2fsprogs-${version}.tar.xz#fd5bf388cbdbe006a3d3b318d983b2948382440acc85a87f1e7d108653e8db0b"
)
useconfigure='true'
configopts=(
    'CFLAGS=-std=c17'
)
