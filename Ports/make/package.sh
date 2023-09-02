#!/usr/bin/env -S bash ../.port_include.sh
port='make'
version='4.4.1'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=("build-aux/config.sub")
files=(
    "https://ftpmirror.gnu.org/gnu/make/make-${version}.tar.gz#dd16fb1d67bfab79a72f5e8390735c49e3e8e70b4945a15ab1f81ddb78658fb3"
)
configopts=("--target=${SERENITY_ARCH}-pc-serenity" "--with-sysroot=/" "--without-guile")
