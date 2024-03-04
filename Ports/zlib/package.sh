#!/usr/bin/env -S bash ../.port_include.sh
port='zlib'
version='1.3.1'
useconfigure='true'
files=(
    "https://github.com/madler/zlib/releases/download/v${version}/zlib-${version}.tar.gz#9a93b2b7dfdac77ceba5a558a580e74667dd6fede4585b91eefb60f03b72df23"
)

configure() {
    # No SONAME is set on unknown systems by default. Manually set it
    # to an unversioned name to avoid needing to rebuild dependent
    # ports after a minor version upgrade.
    export LDSHARED="$CC -shared -Wl,-soname,libz.so"
    run ./configure --uname=SerenityOS
}
