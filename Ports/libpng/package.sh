#!/usr/bin/env -S bash ../.port_include.sh
port='libpng'
version='1.6.59'
useconfigure='true'
configopts=(
    '--disable-static'
    '--enable-shared'
)
files=(
    "https://github.com/pnggroup/libpng/archive/refs/tags/v${version}.tar.gz#2540302a1844ad2b2b501977abecfa850f265f97b78f065a712ab4074a89f5b5"
)
depends=(
    'zlib'
)
