#!/usr/bin/env -S bash ../.port_include.sh
port='libiconv'
version='1.19'
files=(
    "mirror://gnu/libiconv/libiconv-${version}.tar.gz#88dd96a8c0464eca144fc791ae60cd31cd8ee78321e67397e25fc095c4a19aa6"
)
useconfigure='true'
configopts=("--enable-shared" "--disable-nls" "CFLAGS=-std=c17")
