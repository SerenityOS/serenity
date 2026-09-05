#!/usr/bin/env -S bash ../.port_include.sh
port='readline'
version='8.3'
depends=('ncurses')
useconfigure='true'
files=(
    "https://ftpmirror.gnu.org/gnu/readline/readline-${version}.tar.gz#fe5383204467828cd495ee8d1d3c037a7eba1389c22bc6a041f627976f9061cc"
)
configopts=(
    '--disable-static'
    '--enable-shared'
    '--with-curses'
    '--with-shared-termcap-library'
)
