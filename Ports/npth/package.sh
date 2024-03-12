#!/usr/bin/env -S bash ../.port_include.sh
port=npth
version=1.7
useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("build-aux/config.sub")
files=(
    "https://gnupg.org/ftp/gcrypt/npth/npth-${version}.tar.bz2#8589f56937b75ce33b28d312fccbf302b3b71ec3f3945fde6aaa74027914ad05"
)

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" "${configopts[@]}"
}
