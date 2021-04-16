#!/usr/bin/env -S bash ../.port_include.sh
port=stress-ng
version=0.11.23
files="https://github.com/ColinIanKing/stress-ng/archive/V${version}.tar.gz stress-ng-${version}.tar.gz 49fa5547c8cfd7871b9d6261cce6ace3"
auth_type=md5
depends=zlib

pre_configure() {
    export CFLAGS="-I${SERENITY_BUILD_DIR}/Root/usr/local/include"
    export LDFLAGS="-L${SERENITY_BUILD_DIR}/Root/usr/local/lib -lzlib"

}
