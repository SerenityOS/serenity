#!/usr/bin/env -S bash ../.port_include.sh
port='pkgconf'
version='3.0.11'
files=(
    "https://distfiles.ariadne.space/pkgconf/pkgconf-${version}.tar.xz#d0cd841bc8e55b9da888106c00dc256d05b7d1d8e5a46f4be4eb4c5125bd2dc4"
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
