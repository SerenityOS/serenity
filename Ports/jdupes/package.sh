#!/usr/bin/env -S bash ../.port_include.sh
port='jdupes'
version='1.28.0'
files=(
    "https://codeberg.org/jbruchon/jdupes/archive/v${version}.tar.gz#a8f21c04fff5e3ff0a92e8ac76114b2195ed43dc32b84bf343f5256e7ba9cb04"
)
workdir='jdupes'
depends=(
    'libjodycode'
)
makeopts+=(
    'UNAME_S=serenity'
)
installopts+=(
    'DISABLE_DEDUPE=1'
)

export LDFLAGS='-z noexecstack'
