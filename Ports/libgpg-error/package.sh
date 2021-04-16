#!/usr/bin/env -S bash ../.port_include.sh
port=libgpg-error
version=1.42
useconfigure=true
configopts="--disable-tests --disable-threads"
files="https://gnupg.org/ftp/gcrypt/libgpg-error/libgpg-error-${version}.tar.bz2 libgpg-error-${version}.tar.bz2 133fed221ba8f63f5842858a1ff67cb3"
auth_type=md5

pre_configure() {
    export gcry_cv_gcc_has_f_visibility=no
}

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" $configopts
}
