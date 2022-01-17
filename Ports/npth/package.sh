#!/usr/bin/env -S bash ../.port_include.sh
port=npth
version=1.6
useconfigure=true
use_fresh_config_sub=true
config_sub_path=build-aux/config.sub
files="https://gnupg.org/ftp/gcrypt/npth/npth-${version}.tar.bz2 npth-${version}.tar.bz2 1393abd9adcf0762d34798dc34fdcf4d0d22a8410721e76f1e3afcd1daa4e2d1"
auth_type=sha256

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" --build="$($workdir/build-aux/config.guess)" "${configopts[@]}"
}

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -pthread -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libnpth.so -Wl,-soname,libnpth.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libnpth.a -Wl,--no-whole-archive
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libnpth.la
}
