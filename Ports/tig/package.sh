#!/usr/bin/env -S bash ../.port_include.sh
port='tig'
version='2.5.8'
useconfigure='true'
files=(
    "https://github.com/jonas/tig/releases/download/tig-${version}/tig-${version}.tar.gz#b70e0a42aed74a4a3990ccfe35262305917175e3164330c0889bd70580406391"
)
depends=(
    'libiconv'
    'ncurses'
    'pcre'
    'readline'
)
