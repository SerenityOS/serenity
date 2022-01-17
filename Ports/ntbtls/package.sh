#!/usr/bin/env -S bash ../.port_include.sh
port=ntbtls
version=0.2.0
useconfigure=true
use_fresh_config_sub=true
config_sub_path=build-aux/config.sub
depends=("libgpg-error" "libksba" "libgcrypt")
files="https://gnupg.org/ftp/gcrypt/ntbtls/ntbtls-${version}.tar.bz2 ntbtls-${version}.tar.bz2 649fe74a311d13e43b16b26ebaa91665ddb632925b73902592eac3ed30519e17"
auth_type=sha256

pre_configure() {
    export ntbtls_cv_gcc_has_f_visibility=no
}

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" "${configopts[@]}"
}

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libntbtls.so -Wl,-soname,libntbtls.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libntbtls.a -Wl,--no-whole-archive -lgpg-error -lksba -lgcrypt
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libntbtls.la
}
