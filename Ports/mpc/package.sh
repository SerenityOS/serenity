#!/usr/bin/env -S bash ../.port_include.sh
port=mpc
version=1.2.1
useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("build-aux/config.sub")
configopts=("--target=${SERENITY_ARCH}-pc-serenity" "--with-sysroot=${SERENITY_INSTALL_ROOT}")
files=(
    "https://ftpmirror.gnu.org/gnu/mpc/mpc-${version}.tar.gz mpc-${version}.tar.gz 17503d2c395dfcf106b622dc142683c1199431d095367c6aacba6eec30340459"
)
depends=("gmp" "mpfr")
