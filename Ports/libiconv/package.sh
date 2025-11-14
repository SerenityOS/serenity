#!/usr/bin/env -S bash ../.port_include.sh
port='libiconv'
version='1.18'
files=(
    "https://ftpmirror.gnu.org/gnu/libiconv/libiconv-${version}.tar.gz#3b08f5f4f9b4eb82f151a7040bfd6fe6c6fb922efe4b1659c66ea933276965e8"
)
useconfigure='true'
configopts=("--enable-shared" "--disable-nls" "CFLAGS=-std=c17")
