#!/usr/bin/env -S bash ../.port_include.sh
port=ntbtls
version=0.2.0
useconfigure=true
#configopts="--with-libgpg-error-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
files="https://gnupg.org/ftp/gcrypt/ntbtls/ntbtls-${version}.tar.bz2 ntbtls-${version}.tar.bz2 efe1b12502df319bf78707a2fa767098"
auth_type=md5

pre_configure() {
    export ntbtls_cv_gcc_has_f_visibility=no
}

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" $configopts
}
