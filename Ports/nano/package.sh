#!/usr/bin/env -S bash ../.port_include.sh
port='nano'
version='9.3'
files=(
    "https://www.nano-editor.org/dist/v9/nano-${version}.tar.xz#a9affc52c9596ee0682bcdd4ceb3c0b235eb221d83d733dfd9b0dff45897cb0d"
)
useconfigure='true'
configopts=("--disable-utf8")
depends=("ncurses")
