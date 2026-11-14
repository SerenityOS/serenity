#!/usr/bin/env -S bash ../.port_include.sh
port='zlib'
version='1.4'
useconfigure='true'
files=(
    "https://github.com/madler/zlib/releases/download/v${version}/zlib-${version}.tar.gz#02dd159783677a749c04adbdd02b55b8a2bcc7e054f51b76b549199a916ad584"
)

configure() {
    # No SONAME is set on unknown systems by default. Manually set it
    # to an unversioned name to avoid needing to rebuild dependent
    # ports after a minor version upgrade.
    export LDSHARED="$CC -shared -Wl,-soname,libz.so"
    run ./configure --uname=SerenityOS
}
