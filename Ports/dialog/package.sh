#!/usr/bin/env -S bash ../.port_include.sh
port='dialog'
version='1.3-20260721'
files=(
    "https://invisible-mirror.net/archives/dialog/dialog-${version}.tgz#62bdf59057d4f760a1cc2217827f07887b4a3eebf694c25eacd4803d2171cdc6"
)
useconfigure='true'
use_fresh_config_sub='true'
configopts=("--prefix=/usr/local" "--with-ncurses" "--with-curses-dir=${SERENITY_INSTALL_ROOT}/usr/local/include/ncurses")
depends=("ncurses")
