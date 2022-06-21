#!/usr/bin/env -S bash ../.port_include.sh
port=mc
version=4.8.28
useconfigure=true
files="http://ftp.midnight-commander.org/mc-${version}.tar.xz ${port}-${version}.tar.xz e994d9be9a7172e9ac4a4ad62107921f6aa312e668b056dfe5b8bcebbaf53803"
auth_type=sha256
depends=("gettext" "glib" "libtool" "ncurses" "vim")
configopts=(
    "--disable-largefile"
    "--disable-vfs"
    "--without-edit"
    "--without-x"
    "--with-homedir"
    "--with-screen=ncurses"
    "--with-ncurses-includes=$SERENITY_BUILD_DIR/Root/usr/local/include/ncurses"
    "--with-ncurses-libs=$SERENITY_BUILD_DIR/Root/usr/local/lib"
)
use_fresh_config_sub=true
config_sub_paths=("config/config.sub")
