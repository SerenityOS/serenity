#!/usr/bin/env -S bash ../.port_include.sh
port='patch'
version='2.8.1'
useconfigure='true'
files=(
    "mirror://gnu/patch/patch-${version}.tar.gz#0f7b43c7ea450c5d3e9eaf8301177628bc0171d580b2bd6a72822409767144ff"
)
