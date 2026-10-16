#!/usr/bin/env -S bash ../.port_include.sh
port='pkgconf'
version='3.0.9'
files=(
    "https://distfiles.ariadne.space/pkgconf/pkgconf-${version}.tar.xz#6ab2d3b8087dcf500e34003a6a8934dca55d0a3d6066aeafb03c352db3e68bfd"
)
useconfigure='true'
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
