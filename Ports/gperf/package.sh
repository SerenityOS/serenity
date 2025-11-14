#!/usr/bin/env -S bash ../.port_include.sh
port='gperf'
version='3.3'
useconfigure='true'
files=(
    "https://ftpmirror.gnu.org/gnu/gperf/gperf-${version}.tar.gz#fd87e0aba7e43ae054837afd6cd4db03a3f2693deb3619085e6ed9d8d9604ad8"
)
configopts=("--prefix=/usr/local")
