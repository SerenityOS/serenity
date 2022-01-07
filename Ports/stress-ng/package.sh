#!/usr/bin/env -S bash ../.port_include.sh
port=stress-ng
version=0.13.10
files="https://github.com/ColinIanKing/stress-ng/archive/V${version}.tar.gz stress-ng-${version}.tar.gz 972b429f9eb0afbceabf7f3babab8599d8224b5d146e244c2cfe65129befb973"
auth_type=sha256
depends=("zlib")

pre_configure() {
    export CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include"
    export LDFLAGS="-L${SERENITY_INSTALL_ROOT}/usr/local/lib -lzlib"

}
