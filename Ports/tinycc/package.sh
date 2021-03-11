#!/usr/bin/env -S bash ../.port_include.sh
port=tinycc
version=dev
files="https://github.com/TinyCC/tinycc/archive/dev.tar.gz tinycc-dev.tar.gz"
useconfigure=true
makeopts=tcc

configure() {
    run ./configure \
        --cross-prefix="${SERENITY_ARCH}-pc-serenity-" \
        --cpu="${SERENITY_ARCH}" \
        --triplet="${SERENITY_ARCH}-pc-serenity" \
        --crtprefix=/usr/lib
}

export CONFIG_ldl=no
