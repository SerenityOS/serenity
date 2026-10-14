#!/usr/bin/env -S bash ../.port_include.sh
port='groff'
version='1.24.2'
useconfigure='true'
files=(
    "mirror://gnu/groff/groff-${version}.tar.gz#f9c1efd5bebbe37fc6e1063db7473ce8df1e3e0be4ff0f43ce04fce57e9c5dd9"
)
depends=(
    'libiconv'
)
