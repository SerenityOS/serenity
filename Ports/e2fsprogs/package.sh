#!/usr/bin/env -S bash ../.port_include.sh
port='e2fsprogs'
version='1.46.5'
files=(
    "https://www.kernel.org/pub/linux/kernel/people/tytso/e2fsprogs/v${version}/e2fsprogs-${version}.tar.xz 2f16c9176704cf645dc69d5b15ff704ae722d665df38b2ed3cfc249757d8d81e"
)
useconfigure='true'
