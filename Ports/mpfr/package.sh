#!/usr/bin/env -S bash ../.port_include.sh
port='mpfr'
version='4.2.1'
useconfigure='true'
use_fresh_config_sub='true'
configopts=(
    "--target=${SERENITY_ARCH}-pc-serenity"
    '--with-sysroot=/'
)
files=(
    "https://ftpmirror.gnu.org/gnu/mpfr/mpfr-${version}.tar.xz#277807353a6726978996945af13e52829e3abd7a9a5b7fb2793894e18f1fcbb2"
)
depends=(
    'gmp'
)
