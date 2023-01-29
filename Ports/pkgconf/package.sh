#!/usr/bin/env -S bash ../.port_include.sh
port=pkgconf
version=1.8.0
files="https://distfiles.dereferenced.org/pkgconf/pkgconf-${version}.tar.xz pkgconf-${version}.tar.xz ef9c7e61822b7cb8356e6e9e1dca58d9556f3200d78acab35e4347e9d4c2bbaf"
auth_type=sha256
useconfigure=true
use_fresh_config_sub=true
# FIXME: This looks suspiciously host-y...
configopts=("--prefix=/usr/local" "--with-pkg-config-dir=/usr/local/lib/pkgconfig")

post_install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/bin"
    # FIXME: Same here, what is this about?!
    run ln -sf /usr/local/bin/pkgconf "${SERENITY_INSTALL_ROOT}/usr/local/bin/pkg-config"
}
