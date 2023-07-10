#!/usr/bin/env -S bash ../.port_include.sh
port=zlib
version=1.2.13
useconfigure=true
files=(
    "https://www.zlib.net/zlib-${version}.tar.gz zlib-${version}.tar.gz b3a24de97a8fdbc835b9833169501030b8977031bcb54b3b3ac13740f846ab30"
)

configure() {
    # No SONAME is set on unknown systems by default. Manually set it
    # to an unversioned name to avoid needing to rebuild dependent
    # ports after a minor version upgrade.
    export LDSHARED="$CC -shared -Wl,-soname,libz.so"
    run ./configure --uname=SerenityOS
}
