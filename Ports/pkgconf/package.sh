#!/usr/bin/env -S bash ../.port_include.sh
port=pkgconf
version=1.7.3
files="https://distfiles.dereferenced.org/pkgconf/pkgconf-${version}.tar.xz pkgconf-${version}.tar.xz b846aea51cf696c3392a0ae58bef93e2e72f8e7073ca6ad1ed8b01c85871f9c0"
auth_type=sha256
useconfigure=true
# FIXME: This looks suspiciously host-y... 
configopts="--prefix=/usr/local --with-pkg-config-dir=/usr/local/lib/pkgconfig"

post_install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/bin"
    # FIXME: Same here, what is this about?!
    run ln -sf /usr/local/bin/pkgconf "${SERENITY_INSTALL_ROOT}/usr/local/bin/pkg-config"
}
