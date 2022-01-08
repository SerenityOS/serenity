#!/usr/bin/env -S bash ../.port_include.sh
port=tiff
version=4.3.0
useconfigure=true
files="http://download.osgeo.org/libtiff/tiff-${version}.tar.gz tiff-${version}.tar.gz 0e46e5acb087ce7d1ac53cf4f56a09b221537fc86dfc5daaad1c2e89e1b37ac8"
auth_type="sha256"
depends=("libjpeg" "zstd" "xz")

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libtiff.so -Wl,-soname,libtiff.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libtiff.a -Wl,--no-whole-archive -lzstd -llzma -ljpeg
    ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/libtiffxx.so -Wl,-soname,libtiffxx.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/libtiffxx.a -Wl,--no-whole-archive -lzstd -llzma -ljpeg
    rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/libtiff.la ${SERENITY_INSTALL_ROOT}/usr/local/lib/libtiffxx.la
}
