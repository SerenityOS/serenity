#!/usr/bin/env -S bash ../.port_include.sh
port='libiconv'
version='1.17'
files=(
    "https://ftpmirror.gnu.org/gnu/libiconv/libiconv-${version}.tar.gz#8f74213b56238c85a50a5329f77e06198771e70dd9a739779f4c02f65d971313"
)
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=("build-aux/config.sub" "libcharset/build-aux/config.sub")
configopts=("--enable-shared" "--disable-nls")
