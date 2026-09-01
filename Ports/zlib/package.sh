#!/usr/bin/env -S bash ../.port_include.sh
port='zlib'
version='1.3.2'
useconfigure='true'
files=(
    "https://github.com/madler/zlib/releases/download/v${version}/zlib-${version}.tar.gz#bb329a0a2cd0274d05519d61c667c062e06990d72e125ee2dfa8de64f0119d16"
)

configure() {
    # No SONAME is set on unknown systems by default. Manually set it
    # to an unversioned name to avoid needing to rebuild dependent
    # ports after a minor version upgrade.
    export LDSHARED="$CC -shared -Wl,-soname,libz.so"
    run ./configure --uname=SerenityOS
}
