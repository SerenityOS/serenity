#!/usr/bin/env -S bash ../.port_include.sh
port='dialog'
version='1.3-20220526'
files=(
    "https://invisible-mirror.net/archives/dialog/dialog-${version}.tgz#858c9a625b20fde19fb7b19949ee9e9efcade23c56d917b1adb30e98ff6d6b33"
)
useconfigure='true'
use_fresh_config_sub='true'
configopts=("--prefix=/usr/local" "--with-ncurses" "--with-curses-dir=${SERENITY_INSTALL_ROOT}/usr/local/include/ncurses")
depends=("ncurses")
