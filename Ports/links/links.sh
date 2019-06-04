#!/bin/sh
PORT_DIR=links
fetch() {
    run_fetch_web "http://links.twibright.com/download/links-2.19.tar.bz2"
}
configure() {
    run_export_env CC i686-pc-serenity-gcc
    run_configure_autotools
}
build() {
    run_make
}
install() {
    run_make_install DESTDIR="$SERENITY_ROOT"/Root
}
. ../.port_include.sh
