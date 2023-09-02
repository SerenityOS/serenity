#!/usr/bin/env -S bash ../.port_include.sh
port='xorriso'
version='1.5.6'
files=(
    "https://www.gnu.org/software/xorriso/xorriso-${version}.tar.gz#d4b6b66bd04c49c6b358ee66475d806d6f6d7486e801106a47d331df1f2f8feb"
)
depends=(
    'libiconv'
)
useconfigure='true'
use_fresh_config_sub='true'
