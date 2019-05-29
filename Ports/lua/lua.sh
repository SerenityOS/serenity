#!/bin/sh
PORT_DIR=lua
MAKEOPTS='generic'
function fetch() {
    run_fetch_web "http://www.lua.org/ftp/lua-5.3.5.tar.gz"
    run_patch lua.patch -p1
}

function run_make() {
    run_command make $MAKEOPTS "$@"
}

function build() {
    run_make
}
function install() {
    run_make_install DESTDIR="$SERENITY_ROOT"/Root
}
source ../.port_include.sh
