#!/usr/bin/env -S bash ../.port_include.sh
port='mandoc'
version='1.14.6'
useconfigure='true'
files=(
    "https://mandoc.bsd.lv/snapshots/mandoc-${version}.tar.gz#8bf0d570f01e70a6e124884088870cbed7537f36328d512909eb10cd53179d9c"
)
depends=(
    'less'
    'pcre2'
    'zlib'
)
