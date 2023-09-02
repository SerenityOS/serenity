#!/usr/bin/env -S bash ../.port_include.sh
port='lynx'
version='2.8.9rel.1'
files=(
    "https://invisible-island.net/archives/lynx/tarballs/lynx${version}.tar.bz2#387f193d7792f9cfada14c60b0e5c0bff18f227d9257a39483e14fa1aaf79595"
)
workdir="lynx${version}"
depends=('ncurses' 'openssl' 'zlib')
useconfigure='true'
use_fresh_config_sub='true'
configopts=('--without-system-type' '--with-screen=ncurses' '--with-ssl' '--with-zlib')
