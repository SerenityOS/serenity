#!/usr/bin/env -S bash ../.port_include.sh
port=libksba
version=1.5.1
useconfigure=true
files="https://gnupg.org/ftp/gcrypt/libksba/libksba-${version}.tar.bz2 libksba-${version}.tar.bz2 96e207b7adc637a3dbc29bac90312200"
auth_type=md5

pre_configure() {
    export ksba_cv_gcc_has_f_visibility=no
}

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" $configopts
}
