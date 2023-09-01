#!/usr/bin/env -S bash ../.port_include.sh
port='gzip'
version='1.13'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'build-aux/config.sub'
)
files=(
    "https://ftpmirror.gnu.org/gnu/gzip/gzip-${version}.tar.gz#20fc818aeebae87cdbf209d35141ad9d3cf312b35a5e6be61bfcfbf9eddd212a"
)
