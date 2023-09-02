#!/usr/bin/env -S bash ../.port_include.sh
port='potrace'
version='1.16'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('config.sub')
files=(
    "https://potrace.sourceforge.net/download/${version}/potrace-${version}.tar.gz#be8248a17dedd6ccbaab2fcc45835bb0502d062e40fbded3bc56028ce5eb7acc"
)
configopts=(
    "--with-libpotrace"
)
depends=(
    'zlib'
)
