#!/bin/sh
PORT_DIR=doom
fetch() {
    run_fetch_git "https://github.com/ozkl/doomgeneric.git"

    run_patch serenity-port.patch -p1
}
configure() {
    echo ""
}
build() {
    run_make -C doomgeneric/
}
install() {
    run_make_install -C doomgeneric/ DESTDIR="$SERENITY_ROOT"/Root
}
. ../.port_include.sh
