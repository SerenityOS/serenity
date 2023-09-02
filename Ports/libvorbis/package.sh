#!/usr/bin/env -S bash ../.port_include.sh
port=libvorbis
version=1.3.7
useconfigure=true
configopts=("--disable-static" "--enable-shared")
use_fresh_config_sub=true
files=(
    "https://github.com/xiph/vorbis/releases/download/v${version}/libvorbis-${version}.tar.gz#0e982409a9c3fc82ee06e08205b1355e5c6aa4c36bca58146ef399621b0ce5ab"
)
depends=("libogg")

post_install() {
    # Fix up broken libtool paths
    # FIXME: apply a proper libtool fix
    sed_in_place "s# /usr/local# ${SERENITY_INSTALL_ROOT}/usr/local#g" "${SERENITY_INSTALL_ROOT}"/usr/local/lib/libvorbis*.la
}
