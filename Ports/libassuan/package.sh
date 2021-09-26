#!/usr/bin/env -S bash ../.port_include.sh
port=libassuan
version=2.5.5
useconfigure=true
depends=("libgpg-error")
files="https://gnupg.org/ftp/gcrypt/libassuan/libassuan-${version}.tar.bz2 libassuan-${version}.tar.bz2 8e8c2fcc982f9ca67dcbb1d95e2dc746b1739a4668bc20b3a3c5be632edb34e4"
auth_type=sha256

pre_configure() {
    export ac_cv_lib_pthread_pthread_create=no
}

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" "${configopts[@]}"
}

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libassuan.so -Wl,-soname,libassuan.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libassuan.a -Wl,--no-whole-archive -lgpg-error
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libassuan.la
}
