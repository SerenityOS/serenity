#!/usr/bin/env -S bash ../.port_include.sh
port='fio'
version='3.43'
files=(
    "https://brick.kernel.dk/snaps/${port}-${version}.tar.gz#6f57893c7a73d9ed72e9e2b6e15f136a99c137bd6a7645fdeeb4a40b3e9da78d"
)
depends=("zlib")

export LDFLAGS='-ldl'
