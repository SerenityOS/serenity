#!/bin/sh
PORT_DIR=nyancat
fetch() {
    run_fetch_git "https://github.com/klange/nyancat.git"
    run_patch serenity-changes.patch -p1
}
configure() {
    echo
}
build() {
    run_make
}
install() {
    run_make_install DESTDIR="$SERENITY_ROOT"/Root
}
. ../.port_include.sh
