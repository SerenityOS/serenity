#!/bin/bash ../.port_include.sh
port=openssh
workdir=openssh-portable-9ca7e9c861775dd6c6312bc8aaab687403d24676
version=8.3-9ca7e9c
files="https://github.com/openssh/openssh-portable/archive/9ca7e9c861775dd6c6312bc8aaab687403d24676.tar.gz openssh-8.3-9ca7e9c.tar.gz"
depends="zlib openssl"
useconfigure=true
usr_local=$SERENITY_ROOT/Build/Root/usr/local/
configopts="--prefix=/usr/local --disable-utmp --sysconfdir=/etc/ssh --with-ssl-dir=$usr_local/lib"
pre_configure() {
	cd "${workdir}"
	autoreconf
	cd ..
}
install() {
	# Can't make keys outside of Serenity since ssh-keygen is built for Serenity.
	run make DESTDIR="$SERENITY_ROOT"/Build/Root $installopts install-nokeys
}
