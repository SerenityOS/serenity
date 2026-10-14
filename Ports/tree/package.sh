#!/usr/bin/env -S bash ../.port_include.sh
port='tree'
version='2.3.2'
files=(
    "https://github.com/Old-Man-Programmer/tree/archive/refs/tags/${version}.tar.gz#22cf32e84e3eb508d97a9e991c2c3cc006b9dcf4afed201d96311c5c57d08fcf"
)

build() {
    run make all
}

install() {
    run make install PREFIX="${SERENITY_INSTALL_ROOT}/usr/local"
}
