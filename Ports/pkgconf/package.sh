#!/usr/bin/env -S bash ../.port_include.sh
port=pkgconf
version=1.7.3
files="https://distfiles.dereferenced.org/pkgconf/pkgconf-1.7.3.tar.xz pkgconf-1.7.3.tar.xz"
useconfigure=true
usr_local=$SERENITY_ROOT/Build/Root/usr/local/
configopts="--prefix=/usr/local --with-pkg-config-dir=/usr/local/lib/pkgconfig"

post_install() {
    mkdir -p $SERENITY_ROOT/Build/Root/bin
    ln -sf /usr/local/bin/pkgconf $SERENITY_ROOT/Build/Root/usr/local/bin/pkg-config
}
