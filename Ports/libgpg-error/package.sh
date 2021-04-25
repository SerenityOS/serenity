#!/usr/bin/env -S bash ../.port_include.sh
port=libgpg-error
version=1.42
useconfigure=true
configopts="--disable-tests --disable-threads"
files="https://gnupg.org/ftp/gcrypt/libgpg-error/libgpg-error-${version}.tar.bz2 libgpg-error-${version}.tar.bz2 fc07e70f6c615f8c4f590a8e37a9b8dd2e2ca1e9408f8e60459c67452b925e23"
auth_type=sha256

pre_configure() {
    export gcry_cv_gcc_has_f_visibility=no
}

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" $configopts
}
