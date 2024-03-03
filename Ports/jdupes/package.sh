#!/usr/bin/env -S bash ../.port_include.sh
port='jdupes'
version='1.27.3'
files=(
    "https://codeberg.org/jbruchon/jdupes/archive/v${version}.tar.gz#1c75ed30dc95b3b5024019ab2ba3f78a14835c11d5b71249aa94374fde650c16"
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
