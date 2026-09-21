#!/usr/bin/env -S bash ../.port_include.sh
port='jdupes'
version='1.31.2'
files=(
    "https://codeberg.org/jbruchon/jdupes/archive/v${version}.tar.gz#a003ba9c57f2fbfc30f5af5a886b12423e0a0eba008429a48506d0c31a807c17"
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
