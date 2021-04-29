#!/usr/bin/env -S bash ../.port_include.sh
port=tiff
version=4.2.0
useconfigure=true
files="http://download.osgeo.org/libtiff/tiff-${version}.tar.gz tiff-${version}.tar.gz
http://download.osgeo.org/libtiff/tiff-${version}.tar.gz.sig tiff-${version}.tar.gz.sig"
auth_type="sig"
auth_import_key="EBDFDB21B020EE8FD151A88DE301047DE1198975"
auth_opts="tiff-${version}.tar.gz.sig tiff-${version}.tar.gz"

install() {
    run make DESTDIR=$DESTDIR $installopts install
    ${CC} -shared -o $DESTDIR/usr/local/lib/libtiff.so -Wl,--whole-archive $DESTDIR/usr/local/lib/libtiff.a -Wl,--no-whole-archive
    ${CC} -shared -o $DESTDIR/usr/local/lib/libtiffxx.so -Wl,--whole-archive $DESTDIR/usr/local/lib/libtiffxx.a -Wl,--no-whole-archive
    rm -f $DESTDIR/usr/local/lib/libtiff.la $DESTDIR/usr/local/lib/libtiffxx.la
}
