#!/usr/bin/env -S bash ../.port_include.sh

port='lowdown'
version='3.2.1'
workdir="lowdown-VERSION_${version//./_}"
files=(
    "https://github.com/kristapsdz/lowdown/archive/refs/tags/VERSION_${version//./_}.tar.gz#8501a5efb35b61dc73eabb54a099e21ac1dfaec347bb9c8090660233bcf36dea"
)
useconfigure='true'

configure() {
    run ./configure
}

build() {
    run bmake -j"${MAKEJOBS}"
}

install() {
    run bmake DESTDIR="${SERENITY_INSTALL_ROOT}" install
}
