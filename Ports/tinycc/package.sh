#!/usr/bin/env -S bash ../.port_include.sh
port=tinycc
version=dev
files="https://github.com/TinyCC/tinycc/archive/dev.tar.gz tinycc-dev.tar.gz 1e16fd9926e8e2662a35c790b7c56e8e7e8769c6a8a86a59a534c26046d0d83e"
auth_type=sha256
useconfigure=true
makeopts=("tcc")

configure() {
    run ./configure \
        --cross-prefix="${SERENITY_ARCH}-pc-serenity-" \
        --cpu="${SERENITY_ARCH}" \
        --triplet="${SERENITY_ARCH}-pc-serenity" \
        --crtprefix=/usr/lib
}
