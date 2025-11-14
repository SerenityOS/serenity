#!/usr/bin/env -S bash ../.port_include.sh
port='ed'
version='1.22'
files=(
    "https://ftpmirror.gnu.org/gnu/ed/ed-${version}.tar.lz#7eb22c30a99dcdb50a8630ef7ff3e4642491ac4f8cd1aa9f3182264df4f4ad08"
)
useconfigure='true'
depends=(
    'pcre2'
)

configure() {
    run ./"${configscript}"
}
