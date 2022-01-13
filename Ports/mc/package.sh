#!/usr/bin/env -S bash ../.port_include.sh
port=mc
version=4.8.27
useconfigure=true
files="https://github.com/MidnightCommander/mc/archive/refs/tags/${version}.tar.gz ${port}-${version}.tar.gz 3bab1460d187e1f09409be4bb8550ea7dab125fb9b50036a8dbd2b16e8b1985b"
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

pre_patch() {
    run ./autogen.sh
}
