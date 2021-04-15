#!/usr/bin/env -S bash ../.port_include.sh
port=stress-ng
version=0.11.23
files="https://github.com/ColinIanKing/stress-ng/archive/V${version}.tar.gz stress-ng-${version}.tar.gz"
depends=zlib

pre_configure() {
    export CFLAGS="-I${SERENITY_BUILD_DIR}/Root/usr/local/include"
    export LDFLAGS="-L${SERENITY_BUILD_DIR}/Root/usr/local/lib -lzlib"

}
