#!/usr/bin/env -S bash ../.port_include.sh
port='ed'
version='1.22.6'
files=(
    "mirror://gnu/ed/ed-${version}.tar.lz#3f33b22135219c39c3c695f7b7171c2567d3e2a17c798c0a90607320cbb268f2"
)
useconfigure='true'
depends=(
    'pcre2'
)

configure() {
    run ./"${configscript}"
}
