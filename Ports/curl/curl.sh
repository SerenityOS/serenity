#!/bin/sh
PORT_DIR=curl
fetch() {
    run_fetch_web "https://curl.haxx.se/download/curl-7.65.3.tar.bz2"
    run_patch fix-autoconf.patch -p1
}
configure() {
    run_export_env CC i686-pc-serenity-gcc
    run_configure_autotools --disable-threaded-resolver
}
build() {
    run_make
}
install() {
    run_make_install DESTDIR="$SERENITY_ROOT"/Root
}
. ../.port_include.sh
