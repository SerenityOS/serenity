#!/usr/bin/env -S bash ../.port_include.sh
port='readline'
version='8.2'
depends=('ncurses')
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'support/config.sub'
)
files=(
    "https://ftpmirror.gnu.org/gnu/readline/readline-${version}.tar.gz#3feb7171f16a84ee82ca18a36d7b9be109a52c04f492a053331d7d1095007c35"
)
configopts=(
    '--disable-static'
    '--enable-shared'
    '--with-curses'
    '--with-shared-termcap-library'
)
