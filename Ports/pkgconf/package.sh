#!/usr/bin/env -S bash ../.port_include.sh
port='pkgconf'
version='3.0.8'
files=(
    "https://distfiles.ariadne.space/pkgconf/pkgconf-${version}.tar.xz#943f0bc88bcaeceb630b00262e2a8ad5ea4fac3fba72a4267d8b9ec6b35d1194"
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
