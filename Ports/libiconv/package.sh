#!/usr/bin/env -S bash ../.port_include.sh
port='libiconv'
version='1.19.1'
files=(
    "mirror://gnu/libiconv/libiconv-${version}.tar.gz#9a06844846d664d7122161700c10a64480d7c7bcead3091925a785b38c91edab"
)
useconfigure='true'
configopts=("--enable-shared" "--disable-nls" "CFLAGS=-std=c17")
