#!/usr/bin/env -S bash ../.port_include.sh
port='fio'
version='3.42'
files=(
    "https://brick.kernel.dk/snaps/${port}-${version}.tar.gz#9128d0c81bd7bffab0dd06cbfb755a05ef92f3b8a0b0c61f1b3538df6750f1e0"
)
depends=("zlib")

export LDFLAGS='-ldl'
