#!/usr/bin/env -S bash ../.port_include.sh
port=libjpeg
version=9e
useconfigure=true
files="https://ijg.org/files/jpegsrc.v${version}.tar.gz jpeg-${version}.tar.gz 4077d6a6a75aeb01884f708919d25934c93305e49f7e3f36db9129320e6f4f3d"
auth_type=sha256
workdir="jpeg-$version"

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libjpeg.so -Wl,-soname,libjpeg.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libjpeg.a -Wl,--no-whole-archive
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libjpeg.la
}
