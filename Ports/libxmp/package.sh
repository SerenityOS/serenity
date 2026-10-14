#!/usr/bin/env -S bash ../.port_include.sh
port='libxmp'
version='4.7.4'
files=(
    "https://github.com/libxmp/libxmp/releases/download/libxmp-${version}/libxmp-${version}.tar.gz#a25583aa3b031c78ba0b4e83387fa596fb05742622e60f60d1540ee2af79b8d9"
)
useconfigure='true'
