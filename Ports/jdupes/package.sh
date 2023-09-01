#!/usr/bin/env -S bash ../.port_include.sh
port='jdupes'
version='1.27.3'
files=(
    "https://github.com/jbruchon/jdupes/archive/refs/tags/v${version}.tar.gz#6e8352f61b3920a2b5626c7122c3b80b4fdcc5cdd3f1c0c3424530425a77d846"
)
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
