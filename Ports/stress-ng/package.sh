#!/usr/bin/env -S bash ../.port_include.sh
port=stress-ng
version=0.11.23
files="https://github.com/ColinIanKing/stress-ng/archive/V${version}.tar.gz stress-ng-${version}.tar.gz ffa1c516e3098a1d7ae6a4fd48c6fb41b8dfaabda22aaeebb569d24875870216"
auth_type=sha256
depends=("zlib")

pre_configure() {
    export CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include"
    export LDFLAGS="-L${SERENITY_INSTALL_ROOT}/usr/local/lib -lzlib"

}
