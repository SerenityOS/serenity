#!/usr/bin/env -S bash ../.port_include.sh
port=stress-ng
version=0.13.12
files="https://github.com/ColinIanKing/stress-ng/archive/V${version}.tar.gz stress-ng-${version}.tar.gz 16540d9cfa80d6a274fc0238d7251675ee38df6d5be805d14a67ce9efcb59ce9"
auth_type=sha256
depends=("zlib")

pre_configure() {
    export CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include"
    export LDFLAGS="-L${SERENITY_INSTALL_ROOT}/usr/local/lib -lzlib"

}
