#!/usr/bin/env -S bash ../.port_include.sh
port='patch'
version='2.8'
useconfigure='true'
files=(
    "mirror://gnu/patch/patch-${version}.tar.gz#308a4983ff324521b9b21310bfc2398ca861798f02307c79eb99bb0e0d2bf980"
)
