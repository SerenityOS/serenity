#!/usr/bin/env -S bash ../.port_include.sh
port=npth
version=1.6
useconfigure=true
files="https://gnupg.org/ftp/gcrypt/npth/npth-${version}.tar.bz2 npth-${version}.tar.bz2"

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" $configopts
}
