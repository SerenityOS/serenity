#!/usr/bin/env -S bash ../.port_include.sh
port=dialog
version=1.3-20210324
depends=("ncurses")
files="https://invisible-mirror.net/archives/dialog/dialog-${version}.tgz dialog-${version}.tgz
https://invisible-mirror.net/archives/dialog/dialog-${version}.tgz.asc dialog-${version}.tgz.asc"
auth_type="sig"
auth_import_key="C52048C0C0748FEE227D47A2702353E0F7E48EDB"
auth_opts=("dialog-${version}.tgz.asc" "dialog-${version}.tgz")
useconfigure=true
use_fresh_config_sub=true
configopts=("--prefix=/usr/local" "--with-ncurses" "--with-curses-dir=${SERENITY_INSTALL_ROOT}/usr/local/include/ncurses")
