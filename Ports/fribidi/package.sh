#!/usr/bin/env -S bash ../.port_include.sh
port=fribidi
version=1.0.10
useconfigure=true
files="https://github.com/fribidi/fribidi/releases/download/v${version}/fribidi-${version}.tar.xz fribidi-${version}.tar.xz"
makeopts="CFLAGS=-std=c99"

pre_configure() {
    cd "fribidi-${version}"
    aclocal
    cd ..
}
