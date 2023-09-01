#!/usr/bin/env -S bash ../.port_include.sh
port='libpng'
version='1.6.40'
useconfigure='true'
configopts=(
    '--disable-static'
    '--enable-shared'
)
use_fresh_config_sub='true'
files=(
    "https://download.sourceforge.net/libpng/libpng-${version}.tar.gz#8f720b363aa08683c9bf2a563236f45313af2c55d542b5481ae17dd8d183bb42"
)
depends=(
    'zlib'
)
