#!/bin/sh
PORT_DIR=make
fetch() {
    run_fetch_web "https://ftp.gnu.org/gnu/make/make-4.2.tar.bz2"

    run_patch make-4.2-serenity.patch -p1
}

configure() {
    run_configure_autotools \
        --target=i686-pc-serenity \
        --with-sysroot=/
}
build() {
    run_make
}
install() {
    run_make_install DESTDIR="$SERENITY_ROOT"/Root
}
. ../.port_include.sh
