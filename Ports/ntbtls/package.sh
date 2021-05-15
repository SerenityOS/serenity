#!/usr/bin/env -S bash ../.port_include.sh
port=ntbtls
version=0.2.0
useconfigure=true
#configopts="--with-libgpg-error-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
files="https://gnupg.org/ftp/gcrypt/ntbtls/ntbtls-${version}.tar.bz2 ntbtls-${version}.tar.bz2 649fe74a311d13e43b16b26ebaa91665ddb632925b73902592eac3ed30519e17"
auth_type=sha256

pre_configure() {
    export ntbtls_cv_gcc_has_f_visibility=no
}

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" $configopts
}
