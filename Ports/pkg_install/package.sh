#!/usr/bin/env -S bash ../.port_include.sh

port="pkg_install"
version="20210710"
useconfigure="true"
workdir=${port}
depends="libnbcompat libarchive"
configopts="LIBS=-lnbcompat --enable-bootstrap"

fetch() {
	true
}

build() {
	(cd ${workdir} && bmake STATIC_LIBARCHIVE="${DESTDIR}/usr/local/lib/libarchive.a" \
	STATIC_LIBARCHIVE_LDADD="-lpcreposix -lpcre -lz")
}

install() {
	run bmake install DESTDIR=${DESTDIR}
}
