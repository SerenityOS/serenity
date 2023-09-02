#!/usr/bin/env -S bash ../.port_include.sh
port='zlib'
version='1.3'
useconfigure='true'
files=(
    "https://www.zlib.net/zlib-${version}.tar.gz#ff0ba4c292013dbc27530b3a81e1f9a813cd39de01ca5e0f8bf355702efa593e"
)

configure() {
    # No SONAME is set on unknown systems by default. Manually set it
    # to an unversioned name to avoid needing to rebuild dependent
    # ports after a minor version upgrade.
    export LDSHARED="$CC -shared -Wl,-soname,libz.so"
    run ./configure --uname=SerenityOS
}
