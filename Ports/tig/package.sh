#!/usr/bin/env -S bash ../.port_include.sh
port='tig'
version='2.6.1'
useconfigure='true'
files=(
    "https://github.com/jonas/tig/releases/download/tig-${version}/tig-${version}.tar.gz#5adeabdcd93aa0423d618da8b878b53482bef6e0e9e1fe224acc0f18031fe91e"
)
depends=(
    'libiconv'
    'ncurses'
    'pcre'
    'readline'
)
