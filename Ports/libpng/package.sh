#!/usr/bin/env -S bash ../.port_include.sh
port='libpng'
version='1.6.43'
useconfigure='true'
configopts=(
    '--disable-static'
    '--enable-shared'
)
use_fresh_config_sub='true'
files=(
    "https://download.sourceforge.net/libpng/libpng-${version}.tar.gz#e804e465d4b109b5ad285a8fb71f0dd3f74f0068f91ce3cdfde618180c174925"
)
depends=(
    'zlib'
)
