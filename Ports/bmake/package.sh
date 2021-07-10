#!/usr/bin/env -S bash ../.port_include.sh

port="bmake"
version="20210420"
useconfigure="true"
files="https://www.crufty.net/ftp/pub/sjg/bmake-${version}.tar.gz bmake-${version}.tar.gz"
workdir=${port}
configopts=--without-makefile

build() {
	echo "#define NO_REGEX" >> ${workdir}/config.h
	run bash ./make-bootstrap.sh
}

install() {
	run cp bmake ${DESTDIR}/usr/local/bin
}