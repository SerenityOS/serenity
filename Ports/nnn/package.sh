#!/usr/bin/env -S bash ../.port_include.sh
port='nnn'
version='5.3'
files=(
    "https://github.com/jarun/nnn/releases/download/v${version}/nnn-v${version}.tar.gz#c626d57d0aaa5effe04a7e8c24ec9000bc3aca012461b821ecd3c81f5131bb6d"
)
depends=(
    'gettext'
    'libfts'
    'ncurses'
    'readline'
)
