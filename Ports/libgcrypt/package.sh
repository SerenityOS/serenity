#!/usr/bin/env -S bash ../.port_include.sh
port=libgcrypt
version=1.9.2
useconfigure=true
configopts="--with-libgpg-error-prefix=${SERENITY_BUILD_DIR}/Root/usr/local"
files="https://gnupg.org/ftp/gcrypt/libgcrypt/libgcrypt-${version}.tar.bz2 libgcrypt-${version}.tar.bz2"

pre_configure() {
    export gcry_cv_gcc_has_f_visibility=no
    export ac_cv_lib_pthread_pthread_create=no
}

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" $configopts
}
