#!/usr/bin/env -S bash ../.port_include.sh

port="pkgsrc"
version="2021Q2"
useconfigure=false
files="https://cdn.NetBSD.org/pub/pkgsrc/pkgsrc-${version}/pkgsrc.tar.gz pkgsrc.tar.gz"
workdir=${port}
depends="bmake"

build() {
	run rm -rf work || true
	run cp ../files/Serenity.mk mk/platform/
	run cp ../files/serenity_bootstrap.sh bootstrap/
	echo ${DESTDIR}
	run bash -c "opsys=Serenity HOST=i686-pc-serenity DESTDIR=${DESTDIR} bootstrap/serenity_bootstrap.sh --prefix=/usr/pkg --unprivileged"
}

install() {
	run cp work/bin/install-sh ${DESTDIR}/usr/local/bin/
	run cp work/bin/nawk ${DESTDIR}/usr/local/bin/
	run cp work/bin/sed ${DESTDIR}/usr/local/bin/
	run cp work/sbin/pkg_add ${DESTDIR}/usr/local/bin/
	run cp work/sbin/pkg_info ${DESTDIR}/usr/local/bin/
	run cp work/sbin/pkg_admin ${DESTDIR}/usr/local/bin/
	run cp work/sbin/pkg_create ${DESTDIR}/usr/local/bin/
	run mkdir -p ${DESTDIR}/usr/pkg/etc
	run cp work/mk.conf.example ${DESTDIR}/usr/pkg/etc/mk.conf
}	
