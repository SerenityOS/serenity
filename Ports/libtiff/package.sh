#!/usr/bin/env -S bash ../.port_include.sh
port=tiff
version=4.2.0
useconfigure=true
files="http://download.osgeo.org/libtiff/tiff-${version}.tar.gz tiff-${version}.tar.gz
http://download.osgeo.org/libtiff/tiff-${version}.tar.gz.sig tiff-${version}.tar.gz.sig"
auth_type="sig"
auth_import_key="EBDFDB21B020EE8FD151A88DE301047DE1198975"
auth_opts=("tiff-${version}.tar.gz.sig" "tiff-${version}.tar.gz")
depends=("libjpeg" "zstd" "xz")

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libtiff.so -Wl,-soname,libtiff.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libtiff.a -Wl,--no-whole-archive -lzstd -llzma -ljpeg
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libtiffxx.so -Wl,-soname,libtiffxx.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libtiffxx.a -Wl,--no-whole-archive -lzstd -llzma -ljpeg
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libtiff.la ${SERENITY_INSTALL_ROOT}/usr/local/lib/libtiffxx.la
}
