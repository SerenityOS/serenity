#!/usr/bin/env -S bash ../.port_include.sh
port='pkgconf'
version='3.0.10'
files=(
    "https://distfiles.ariadne.space/pkgconf/pkgconf-${version}.tar.xz#9371b2f6372da574d4c6dc96fe9b1463037bcf6afcb6671c1555afde7b17e2de"
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
