#!/usr/bin/env -S bash ../.port_include.sh
port=libXt
version=1.1.3
useconfigure=true
configopts="--disable-malloc0returnsnull"
files="https://www.x.org/releases/individual/lib/libXt-${version}.tar.gz libXt-${version}.tar.gz a56f1c13430f8ee0c040de1e3a7f439c9707d2f9c9b5ceee8bdbb74fddbe5560"
depends="libSM libICE libX11 xproto kbproto"
auth_type="sha256"

post_fetch() {
    # The old version of autotools that libXt was packaged with doesn't
    # understand --with-sysroot when using libtool.
    if [ ! -f "${workdir}/.config.patch_applied" ]; then
        run autoreconf -vfi
    fi
}
