#!/usr/bin/env -S bash ../.port_include.sh
port=stress-ng
version=0.13.09
files="https://github.com/ColinIanKing/stress-ng/archive/V${version}.tar.gz stress-ng-${version}.tar.gz 1b57f2864f562358cb75807c2dca7c13ddee3c94803282b22f75009311967c6c"
auth_type=sha256
depends=("zlib")

pre_configure() {
    export CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include"
    export LDFLAGS="-L${SERENITY_INSTALL_ROOT}/usr/local/lib -lzlib"

}
