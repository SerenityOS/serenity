#!/usr/bin/env -S bash ../.port_include.sh
port='gzip'
version='1.12'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('build-aux/config.sub')
files=(
    "https://ftpmirror.gnu.org/gnu/gzip/gzip-${version}.tar.gz gzip-${version}.tar.gz 5b4fb14d38314e09f2fc8a1c510e7cd540a3ea0e3eb9b0420046b82c3bf41085"
)
