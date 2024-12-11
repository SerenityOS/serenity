#!/usr/bin/env -S bash ../.port_include.sh
port='readline'
version='8.2.13'
depends=('ncurses')
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'support/config.sub'
)
files=(
    "https://ftpmirror.gnu.org/gnu/readline/readline-${version}.tar.gz#0e5be4d2937e8bd9b7cd60d46721ce79f88a33415dd68c2d738fb5924638f656"
)
configopts=(
    '--disable-static'
    '--enable-shared'
    '--with-curses'
    '--with-shared-termcap-library'
)
