#!/usr/bin/env -S bash ../.port_include.sh
port='nnn'
version='4.8'
files=(
    "https://github.com/jarun/nnn/releases/download/v${version}/nnn-v${version}.tar.gz#7027f830329ff3451b844d1f5fbeb1d866bed1af6f24a360d5c51888cb1ae8f0"
)
depends=(
    'gettext'
    'libfts'
    'ncurses'
    'readline'
)
