#!/bin/sh
PORT_DIR=lua
MAKEOPTS='generic'

INSTALLOPTS="INSTALL_TOP=$SERENITY_ROOT/Root/"

fetch() {
    run_fetch_web "http://www.lua.org/ftp/lua-5.3.5.tar.gz"
    run_patch lua.patch -p1
}
configure() {
    run_export_env CC i686-pc-serenity-gcc
}
run_make() {
    run_command make $MAKEOPTS "$@"
}

build() {
    run_make
}
install() {
    run_make_install DESTDIR="$SERENITY_ROOT"/Root
}
. ../.port_include.sh
