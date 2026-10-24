#!/usr/bin/env -S bash ../.port_include.sh
port='jdupes'
version='1.31.3'
files=(
    "https://codeberg.org/jbruchon/jdupes/archive/v${version}.tar.gz#3f76188082330cc28d37788377f0ed06b0ba181fcd4d88827a59f26a0c396168"
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
