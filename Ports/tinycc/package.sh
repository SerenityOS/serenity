#!/usr/bin/env -S bash ../.port_include.sh
port='tinycc'
version='0.9.27'
workdir="tinycc-release_${version//./_}"
files=(
    "https://github.com/TinyCC/tinycc/archive/release_${version//./_}.tar.gz#439760025dcb2df0281566bb2b98fead52cb6bafa697f2147d94d2222ef71363"
)
useconfigure='true'
makeopts=("tcc")

configure() {
    run ./configure \
        --cross-prefix="${SERENITY_ARCH}-serenity-" \
        --cpu="${SERENITY_ARCH}" \
        --triplet="${SERENITY_ARCH}-serenity" \
        --crtprefix=/usr/lib
}
