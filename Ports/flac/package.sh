#!/usr/bin/env -S bash ../.port_include.sh
port='flac'
version='1.5.0'
useconfigure='true'
depends=(
    'libogg'
)
files=(
    "https://downloads.xiph.org/releases/flac/flac-${version}.tar.xz#f2c1c76592a82ffff8413ba3c4a1299b6c7ab06c734dee03fd88630485c2b920"
)
