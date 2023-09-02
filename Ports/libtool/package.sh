#!/usr/bin/env -S bash ../.port_include.sh
port=libtool
version=2.4.7
useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("build-aux/config.sub")
depends=("bash" "sed")
files=(
    "https://ftpmirror.gnu.org/gnu/libtool/libtool-${version}.tar.xz#4f7f217f057ce655ff22559ad221a0fd8ef84ad1fc5fcb6990cecc333aa1635d"
)
configopts=("--prefix=/usr/local")

post_install() {
    mkdir -p "${SERENITY_INSTALL_ROOT}/usr/bin"
    ln -sf /usr/local/bin/sed "${SERENITY_INSTALL_ROOT}/usr/bin/sed"
}
