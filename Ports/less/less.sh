#!/bin/sh
PORT_DIR=less
INSTALLOPTS="DESTDIR=$SERENITY_ROOT/Root/"

function fetch() {
    run_fetch_web "http://ftp.gnu.org/gnu/less/less-530.tar.gz"
}
function configure() {
    run_configure_autotools
}
function build() {
    run_make
}
function install() {
    run_make_install
}
source ../.port_include.sh
