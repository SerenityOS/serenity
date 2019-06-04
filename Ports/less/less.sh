#!/bin/sh
PORT_DIR=less
INSTALLOPTS="DESTDIR=$SERENITY_ROOT/Root/"

fetch() {
    run_fetch_web "http://ftp.gnu.org/gnu/less/less-530.tar.gz"
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
