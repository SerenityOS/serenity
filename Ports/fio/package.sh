#!/usr/bin/env -S bash ../.port_include.sh
port='fio'
version='3.33'
files=(
    "https://brick.kernel.dk/snaps/${port}-${version}.tar.gz#d2410e13e0f379d061d077cc5ae325835bb7c6186aa7bafc1df954cbc9b014fc"
)
depends=("zlib")

export LDFLAGS='-ldl'
