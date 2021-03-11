#!/usr/bin/env -S bash ../.port_include.sh
port=pkgconf
version=1.7.3
files="https://distfiles.dereferenced.org/pkgconf/pkgconf-1.7.3.tar.xz pkgconf-1.7.3.tar.xz"
useconfigure=true
# FIXME: This looks suspiciously host-y... 
configopts="--prefix=/usr/local --with-pkg-config-dir=/usr/local/lib/pkgconfig"

post_install() {
    run mkdir -p "${SERENITY_BUILD_DIR}/Root/bin"
    # FIXME: Same here, what is this about?!
    run ln -sf /usr/local/bin/pkgconf "${SERENITY_BUILD_DIR}/Root/usr/local/bin/pkg-config"
}
