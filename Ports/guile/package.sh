#!/usr/bin/env -S bash ../.port_include.sh
port=guile
version=3.0.9
files="https://ftpmirror.gnu.org/gnu/guile/guile-${version}.tar.gz guile-${version}.tar.gz 18525079ad29a0d46d15c76581b5d91c8702301bfd821666d2e1d13726162811"
depends=("gmp" "libunistring" "libffi" "bdwgc" "libiconv")

useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("build-aux/config.sub")
configopts=("--disable-lto" "--disable-jit")
pre_configure() {
    run autoreconf
}
