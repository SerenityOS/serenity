#!/usr/bin/env -S bash ../.port_include.sh
port='pkgconf'
version='3.0.7'
files=(
    "https://distfiles.ariadne.space/pkgconf/pkgconf-${version}.tar.xz#c926ff491cbd9a331a589160811bd97ab1749b4d5198a519338f2cdfabe6940a"
)
useconfigure='true'
use_fresh_config_sub='true'
# FIXME: This looks suspiciously host-y...
configopts=(
    '--prefix=/usr/local'
    '--with-pkg-config-dir=/usr/local/lib/pkgconfig'
)

post_install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/bin"
    # FIXME: Same here, what is this about?!
    run ln -sf /usr/local/bin/pkgconf "${SERENITY_INSTALL_ROOT}/usr/local/bin/pkg-config"
}
