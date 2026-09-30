#!/usr/bin/env -S bash ../.port_include.sh
port='xz'
version='5.8.4'
depends=(
    'libiconv'
    'zlib'
)
files=(
    "https://tukaani.org/xz/xz-${version}.tar.gz#0014c7886930454fe8bd4228665b51af55eeae560ea135c9c4cd33f55b2591d9"
)
useconfigure='true'
configopts=(
    '--disable-static'
    '--enable-shared'
)
