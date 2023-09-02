#!/usr/bin/env -S bash ../.port_include.sh
port=npth
version=1.6
useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("build-aux/config.sub")
files=(
    "https://gnupg.org/ftp/gcrypt/npth/npth-${version}.tar.bz2#1393abd9adcf0762d34798dc34fdcf4d0d22a8410721e76f1e3afcd1daa4e2d1"
)

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" "${configopts[@]}"
}
