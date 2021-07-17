#!/usr/bin/env -S bash ../.port_include.sh

port="bmake"
version="20210420"
useconfigure="true"
files="https://www.crufty.net/ftp/pub/sjg/bmake-${version}.tar.gz bmake-${version}.tar.gz"
workdir=${port}
configopts="--without-makefile --with-default-sys-path=/usr/local/share/mk"

build() {
	export DESTDIR
	run sed -i 's/LIBS=\"\"/LIBS=-lregex/g' ./make-bootstrap.sh
	run bash ./make-bootstrap.sh
}

install() {
	mkdir -p ../../Build/i686/Root/usr/local/bin || true
	mkdir -p ../../Build/i686/Root/usr/local/share/ || true
	run cp bmake ${DESTDIR}/usr/local/bin
	run cp -r mk ${DESTDIR}/usr/local/share/
}
