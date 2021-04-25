#!/usr/bin/env -S bash ../.port_include.sh
port=libassuan
version=2.5.5
useconfigure=true
#configopts="--with-libgpg-error-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
files="https://gnupg.org/ftp/gcrypt/libassuan/libassuan-${version}.tar.bz2 libassuan-${version}.tar.bz2 8e8c2fcc982f9ca67dcbb1d95e2dc746b1739a4668bc20b3a3c5be632edb34e4"
auth_type=sha256

pre_configure() {
    export gcry_cv_gcc_has_f_visibility=no
    export ac_cv_lib_pthread_pthread_create=no
}

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" $configopts
}
