#!/bin/sh
PORT_DIR=ncurses
INSTALLOPTS="DESTDIR=$SERENITY_ROOT/Root/"

fetch() {
    run_fetch_git "https://github.com/mirror/ncurses.git"
    run_patch allow-serenity-os-ncurses.patch -p1
}
configure() {
    run_configure_autotools
}
build() {
    run_make
}
install() {
    run_make_install
}
. ../.port_include.sh
