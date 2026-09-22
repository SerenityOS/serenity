#!/usr/bin/env -S bash ../.port_include.sh
port='lynx'
version='2.9.3'
files=(
    "https://invisible-island.net/archives/lynx/tarballs/lynx${version}.tar.bz2#174b7f2866a60f3247ba75f5c7dbb10b124aede4a1359312de15f3bfebd2050f"
)
workdir="lynx${version}"
depends=(
    'ncurses'
    'openssl'
    'zlib'
)
useconfigure='true'
configopts=(
    '--with-screen=ncursesw'
    '--with-ssl'
    '--with-zlib'
)
