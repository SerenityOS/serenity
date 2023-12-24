#!/usr/bin/env -S bash ../.port_include.sh
port='gawk'
version='5.3.0'
useconfigure='true'
files=(
    "https://ftpmirror.gnu.org/gnu/gawk/gawk-${version}.tar.gz#378f8864ec21cfceaa048f7e1869ac9b4597b449087caf1eb55e440d30273336"
)
depends=(
    'gmp'
    'mpfr'
)
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
