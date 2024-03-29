#!/usr/bin/env -S bash ../.port_include.sh
port='mpc'
version='1.3.1'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'build-aux/config.sub'
)
configopts=(
    "--target=${SERENITY_ARCH}-pc-serenity"
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
)
files=(
    "https://ftpmirror.gnu.org/gnu/mpc/mpc-${version}.tar.gz#ab642492f5cf882b74aa0cb730cd410a81edcdbec895183ce930e706c1c759b8"
)
depends=(
    'gmp'
    'mpfr'
)
