#!/bin/sh
PORT_DIR=SerenityDOOM
fetch() {
    run_fetch_git "https://github.com/SerenityOS/SerenityDOOM.git"
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
