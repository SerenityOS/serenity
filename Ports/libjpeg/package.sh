#!/usr/bin/env -S bash ../.port_include.sh
port=libjpeg
version=9d
useconfigure=true
use_fresh_config_sub=true
files="https://ijg.org/files/jpegsrc.v${version}.tar.gz jpeg-${version}.tar.gz 2303a6acfb6cc533e0e86e8a9d29f7e6079e118b9de3f96e07a71a11c082fa6a"
auth_type=sha256
workdir="jpeg-$version"

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libjpeg.so -Wl,-soname,libjpeg.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libjpeg.a -Wl,--no-whole-archive
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libjpeg.la
}
