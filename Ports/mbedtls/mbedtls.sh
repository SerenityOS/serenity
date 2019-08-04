#!/bin/sh

PORT_DIR=mbedtls

fetch() {
    run_fetch_web "https://tls.mbed.org/download/mbedtls-2.16.2-apache.tgz"
    run_patch remove-net-from-config.patch -p1
    run_patch add-missing-includes.patch -p1
}

configure() {
    echo "move along, nothing to see here"
}

build() {
    run_export_env CC i686-pc-serenity-gcc
    run_make clean
    run_make CFLAGS=-DPLATFORM_UTIL_USE_GMTIME
}

install() {
    run_make_install DESTDIR="$SERENITY_ROOT"/Root
}

. ../.port_include.sh
