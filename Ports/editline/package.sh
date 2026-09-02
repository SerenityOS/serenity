#!/usr/bin/env -S bash ../.port_include.sh

port='editline'
version='2.1.0'
useconfigure='true'
files=(
    "https://github.com/troglobit/editline/releases/download/${version}/editline-${version}.tar.gz#189e179253c0932d15ce94f53e8cde7a0c38383f39f11f3b92d40cd18839678f"
)
configopts=(
    'CFLAGS=-std=c17'
)
