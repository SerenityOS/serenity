#!/usr/bin/env -S bash ../.port_include.sh
port='pcre'
version='8.45'
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "https://downloads.sourceforge.net/project/pcre/pcre/${version}/pcre-${version}.tar.gz#4e6ce03e0336e8b4a3d6c2b70b1c5e18590a5673a98186da90d4f33c23defc09"
)
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
