#!/usr/bin/env -S bash ../.port_include.sh
port='ncdu'
version='1.22'
files=(
    "https://dev.yorhel.nl/download/ncdu-${version}.tar.gz#0ad6c096dc04d5120581104760c01b8f4e97d4191d6c9ef79654fa3c691a176b"
)
useconfigure='true'
depends=("ncurses")
