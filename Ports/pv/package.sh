#!/usr/bin/env -S bash ../.port_include.sh
port='pv'
version='1.12.7'
useconfigure='true'
configopts=(
    '--disable-nls'
)
files=(
    "https://ivarch.com/s/pv-${version}.tar.gz#5326ed7ee95ef18039390c0b17891ac28985ac7c7a4235070fab767c1fe37bd8"
)
