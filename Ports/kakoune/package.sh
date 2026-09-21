#!/usr/bin/env -S bash ../.port_include.sh
port='kakoune'
version='2026.05.21'
files=(
    "https://github.com/mawww/kakoune/archive/refs/tags/v${version}.tar.gz#7ef778bd199e05977841e6f69aad2bee6cd58fb23b0e4bc265d012d42de17580"
)
depends=(
    'bash'
    'sed'
)
makeopts+=(
    "LDFLAGS=-L${DESTDIR}/usr/local/lib"
)
