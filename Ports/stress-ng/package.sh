#!/usr/bin/env -S bash ../.port_include.sh
port=stress-ng
version=0.14.03
files=(
    "https://github.com/ColinIanKing/stress-ng/archive/V${version}.tar.gz 95012c62883ab5826e6157557a075df98cce3cbce2a48bb40851bcc968a8441a"
)
depends=("zlib")

pre_configure() {
    export CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include"
    export LDFLAGS="-L${SERENITY_INSTALL_ROOT}/usr/local/lib -lzlib"
}
