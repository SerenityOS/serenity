#!/usr/bin/env -S bash ../.port_include.sh
port='ncdu'
version='1.18.1'
files=(
    "https://dev.yorhel.nl/download/ncdu-${version}.tar.gz#7c0fa1eb29d85aaed4ba174164bdbb8f011b5c390d017c57d668fc7231332405"
)
useconfigure='true'
depends=("ncurses")
