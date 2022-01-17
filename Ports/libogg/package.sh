#!/usr/bin/env -S bash ../.port_include.sh
port=libogg
version=1.3.5
useconfigure=true
use_fresh_config_sub=true
files="https://github.com/xiph/ogg/releases/download/v${version}/libogg-${version}.tar.gz libogg-${version}.tar.gz 0eb4b4b9420a0f51db142ba3f9c64b333f826532dc0f48c6410ae51f4799b664"
auth_type=sha256

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libogg.so -Wl,-soname,libogg.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libogg.a -Wl,--no-whole-archive
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libogg.la
}
