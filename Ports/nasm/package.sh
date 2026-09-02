#!/usr/bin/env -S bash ../.port_include.sh
port='nasm'
version='3.02'
files=(
    "https://www.nasm.us/pub/nasm/releasebuilds/${version}/nasm-${version}.tar.gz#f504227b2f529e658d41629075f0503b38d67d790af345f34eba4af60c6a5998"
)
useconfigure='true'
