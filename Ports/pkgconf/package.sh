#!/usr/bin/env -S bash ../.port_include.sh
port='pkgconf'
version='2.0.2'
files=(
    "https://distfiles.ariadne.space/pkgconf/pkgconf-${version}.tar.xz#ea5a25ef8f251eb5377ec0e21c75fb61894433cfbdbf0b2559ba33e4c2664401"
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
