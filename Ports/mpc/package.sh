#!/usr/bin/env -S bash ../.port_include.sh
port='mpc'
version='1.4.1'
useconfigure='true'
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
files=(
    "mirror://gnu/mpc/mpc-${version}.tar.xz#91204cd32f164bd3b7c992d4a6a8ce6519511aadab30f78b6982d0bf8d73e931"
)
depends=(
    'gmp'
    'mpfr'
)
