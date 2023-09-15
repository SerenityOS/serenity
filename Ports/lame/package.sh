#!/usr/bin/env -S bash ../.port_include.sh
port='lame'
version='3.100'
useconfigure='true'
use_fresh_config_sub='true'
configopts=(
    '--disable-static'
    '--enable-shared'
    '--enable-dynamic-frontends'
)
files=(
    "https://downloads.sourceforge.net/project/lame/lame/${version}/lame-${version}.tar.gz#ddfe36cab873794038ae2c1210557ad34857a4b6bdc515785d1da9e175b1da1e"
)
