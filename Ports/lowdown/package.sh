#!/usr/bin/env -S bash ../.port_include.sh

port='lowdown'
version='1.0.2'
workdir="lowdown-VERSION_${version//./_}"
files=(
    "https://github.com/kristapsdz/lowdown/archive/refs/tags/VERSION_${version//./_}.tar.gz#049b7883874f8a8e528dc7c4ed7b27cf7ceeb9ecf8fe71c3a8d51d574fddf84b"
)
useconfigure='true'

configure() {
    run ./configure
}
