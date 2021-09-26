#!/usr/bin/env -S bash ../.port_include.sh
port=libjpeg
version=9d
useconfigure=true
files="https://ijg.org/files/jpegsrc.v${version}.tar.gz jpeg-${version}.tar.gz 6c434a3be59f8f62425b2e3c077e785c9ce30ee5874ea1c270e843f273ba71ee"
auth_type=sha256
workdir="jpeg-$version"

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libjpeg.so -Wl,-soname,libjpeg.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libjpeg.a -Wl,--no-whole-archive
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libjpeg.la
}
