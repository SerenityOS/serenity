#!/usr/bin/env -S bash ../.port_include.sh
port='libffi'
version='3.8.0'
useconfigure='true'
files=(
    "https://github.com/libffi/libffi/releases/download/v${version}/libffi-${version}.tar.gz#7da3e2d9a171eb0a038f592ecad3ff2bb2550f3496d87b3b29ad0cf4430c0db4"
)
