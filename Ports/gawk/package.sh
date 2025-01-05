#!/usr/bin/env -S bash ../.port_include.sh
port='gawk'
version='5.3.1'
useconfigure='true'
files=(
    "https://ftpmirror.gnu.org/gnu/gawk/gawk-${version}.tar.gz#fa41b3a85413af87fb5e3a7d9c8fa8d4a20728c67651185bb49c38a7f9382b1e"
)
depends=(
    'gmp'
    'mpfr'
)
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
