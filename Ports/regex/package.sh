#!/usr/bin/env -S bash ../.port_include.sh

port="regex"
version="alpha3.8p1"
useconfigure="true"
workdir=${port}-${version}
files="https://github.com/garyhouston/regex/archive/refs/tags/${version}.tar.gz regex.tar.gz"
useconfigure=false
makeopts="-j$(nproc) lib"

install() {
        run cp libregex.a ${DESTDIR}/usr/local/lib/
        run cp regex.h ${DESTDIR}/usr/local/include/
}

