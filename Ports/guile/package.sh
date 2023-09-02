#!/usr/bin/env -S bash ../.port_include.sh
port=guile
version=3.0.8
files=(
    "https://ftpmirror.gnu.org/gnu/guile/guile-${version}.tar.gz#f25ae0c26e911af1b5005292d4f56621879f74d6958b30741cf67d8b6feb2016"
)
depends=("gmp" "libunistring" "libffi" "bdwgc" "libiconv")

useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("build-aux/config.sub")
configopts=("--disable-lto" "--disable-jit")
pre_configure() {
    run autoreconf
}
