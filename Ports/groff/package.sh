#!/usr/bin/env -S bash ../.port_include.sh
port='groff'
version='1.25.0'
useconfigure='true'
files=(
    "mirror://gnu/groff/groff-${version}.tar.gz#24d103a07eb79e418963bce206c8771b649871f83bda2fd4c2e65529c1c56921"
)
depends=(
    'libiconv'
)
