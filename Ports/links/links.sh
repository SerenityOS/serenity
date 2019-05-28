#!/bin/sh
PORT_DIR=links
function fetch() {
    run_fetch_web "http://links.twibright.com/download/links-2.19.tar.bz2"
}
function configure() {
    run_export_env CC i686-pc-serenity-gcc
    run_configure_autotools
}
function build() {
    run_make
}
function install() {
    run_make_install DESTDIR="$SERENITY_ROOT"/Root
}
source ../.port_include.sh
