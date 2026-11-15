#!/usr/bin/env -S bash ../.port_include.sh
port='stress-ng'
version='0.22.02'
files=(
    "https://github.com/ColinIanKing/stress-ng/archive/V${version}.tar.gz#860004ce7484311b3296fc52796fbbc34dfb7f644a769bbd4e03935b7a1b0ccb"
)
depends=(
    'zlib'
)

pre_configure() {
    export LDFLAGS="-lzlib"
}
