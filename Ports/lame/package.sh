#!/usr/bin/env -S bash ../.port_include.sh
port='lame'
version='4.0'
useconfigure='true'
use_fresh_config_sub='true'
configopts=(
    '--disable-static'
    '--enable-shared'
    '--enable-dynamic-frontends'
)
files=(
    "https://downloads.sourceforge.net/project/lame/lame/${version}/lame-${version}.tar.gz#3df5124d5ad3a98312ffd7ba6a9b36230e4f8a3e66d3ce0f425e336c32d216eb"
)
