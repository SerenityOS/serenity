#!/usr/bin/env -S bash ../.port_include.sh
port=npth
version=1.6
useconfigure=true
files="https://gnupg.org/ftp/gcrypt/npth/npth-${version}.tar.bz2 npth-${version}.tar.bz2 375d1a15ad969f32d25f1a7630929854"
auth_type=md5

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" $configopts
}
