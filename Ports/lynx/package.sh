#!/usr/bin/env -S bash ../.port_include.sh
port='lynx'
version='2.9.2'
files=(
    "https://invisible-island.net/archives/lynx/tarballs/lynx${version}.tar.bz2#7374b89936d991669e101f4e97f2c9592036e1e8cdaa7bafc259a77ab6fb07ce"
)
workdir="lynx${version}"
depends=('ncurses' 'openssl' 'zlib')
useconfigure='true'
use_fresh_config_sub='true'
configopts=('--without-system-type' '--with-screen=ncurses' '--with-ssl' '--with-zlib')
