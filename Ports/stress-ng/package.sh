#!/usr/bin/env -S bash ../.port_include.sh
port='stress-ng'
version='0.16.04'
files=(
    "https://github.com/ColinIanKing/stress-ng/archive/V${version}.tar.gz#3453719508e9e02c57a736c154408538372d078be7dcf8e0165d37a821cdba45"
)
depends=(
    'zlib'
)

pre_configure() {
    export LDFLAGS="-lzlib"
}
