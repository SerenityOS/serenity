#!/usr/bin/env -S bash ../.port_include.sh
port=mpfr
version=4.1.0
useconfigure=true
use_fresh_config_sub=true
configopts=("--target=${SERENITY_ARCH}-pc-serenity" "--with-sysroot=/")
files=(
    "https://ftpmirror.gnu.org/gnu/mpfr/mpfr-${version}.tar.xz 0c98a3f1732ff6ca4ea690552079da9c597872d30e96ec28414ee23c95558a7f"
)
depends=("gmp")
