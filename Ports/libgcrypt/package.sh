#!/usr/bin/env -S bash ../.port_include.sh
port=libgcrypt
version=1.9.2
useconfigure=true
use_fresh_config_sub=true
config_sub_path=build-aux/config.sub
configopts=("--with-libgpg-error-prefix=${SERENITY_INSTALL_ROOT}/usr/local")
depends=("libgpg-error")
files="https://gnupg.org/ftp/gcrypt/libgcrypt/libgcrypt-${version}.tar.bz2 libgcrypt-${version}.tar.bz2 b2c10d091513b271e47177274607b1ffba3d95b188bbfa8797f948aec9053c5a"
auth_type=sha256

pre_configure() {
    export ac_cv_lib_pthread_pthread_create=no
}

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" "${configopts[@]}"
}

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libgcrypt.so -Wl,-soname,libgcrypt.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libgcrypt.a -Wl,--no-whole-archive -lgpg-error
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libgcrypt.la
}
