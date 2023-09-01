#!/usr/bin/env -S bash ../.port_include.sh
port='e2fsprogs'
version='1.47.0'
files=(
    "https://www.kernel.org/pub/linux/kernel/people/tytso/e2fsprogs/v${version}/e2fsprogs-${version}.tar.xz#144af53f2bbd921cef6f8bea88bb9faddca865da3fbc657cc9b4d2001097d5db"
)
useconfigure='true'
