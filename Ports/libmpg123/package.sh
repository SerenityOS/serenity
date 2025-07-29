#!/usr/bin/env -S bash ../.port_include.sh
port='libmpg123'
version='1.32.4'
useconfigure='true'
workdir="mpg123-${version}"
use_fresh_config_sub='true'
config_sub_paths=(
    'build/config.sub'
)
files=(
    "https://download.sourceforge.net/project/mpg123/mpg123/${version}/mpg123-${version}.tar.bz2#5a99664338fb2f751b662f40ee25804d0c9db6b575dcb5ce741c6dc64224a08a"
)
