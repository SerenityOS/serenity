#!/usr/bin/env -S bash ../.port_include.sh
port=libgpg-error
version=1.44
useconfigure=true
use_fresh_config_sub=true
config_sub_path=build-aux/config.sub
depends=("gettext")
configopts=("--disable-tests" "--disable-threads")
files="https://gnupg.org/ftp/gcrypt/libgpg-error/libgpg-error-${version}.tar.bz2 libgpg-error-${version}.tar.bz2 8e3d2da7a8b9a104dd8e9212ebe8e0daf86aa838cc1314ba6bc4de8f2d8a1ff9"
auth_type=sha256

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" "${configopts[@]}"
}

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libgpg-error.so -Wl,-soname,libgpg-error.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libgpg-error.a -Wl,--no-whole-archive -lintl
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libgpg-error.la
}
