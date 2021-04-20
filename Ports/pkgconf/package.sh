#!/usr/bin/env -S bash ../.port_include.sh
port=pkgconf
version=1.7.3
files="https://distfiles.dereferenced.org/pkgconf/pkgconf-${version}.tar.xz pkgconf-${version}.tar.xz 2a19acafd0eccb61d09a5bbf7ce18c9d"
auth_type=md5
useconfigure=true
# FIXME: This looks suspiciously host-y... 
configopts="--prefix=/usr/local --with-pkg-config-dir=/usr/local/lib/pkgconfig"

post_install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/bin"
    # FIXME: Same here, what is this about?!
    run ln -sf /usr/local/bin/pkgconf "${SERENITY_INSTALL_ROOT}/usr/local/bin/pkg-config"
}
