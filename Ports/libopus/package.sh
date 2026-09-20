#!/usr/bin/env -S bash ../.port_include.sh
port='libopus'
version='1.6.1'
workdir="opus-${version}"
useconfigure='true'
files=(
    "https://downloads.xiph.org/releases/opus/opus-${version}.tar.gz#6ffcb593207be92584df15b32466ed64bbec99109f007c82205f0194572411a1"
)
