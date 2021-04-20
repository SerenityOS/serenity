#!/usr/bin/env -S bash ../.port_include.sh
port=libjpeg
version=9d
useconfigure=true
files="https://ijg.org/files/jpegsrc.v${version}.tar.gz jpeg-${version}.tar.gz ad7e40dedc268f97c44e7ee3cd54548a"
auth_type="md5"
workdir="jpeg-$version"

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} $installopts install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libjpeg.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libjpeg.a -Wl,--no-whole-archive
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libjpeg.la
}
