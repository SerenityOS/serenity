#!/usr/bin/env -S bash ../.port_include.sh
port=openssh
workdir=openssh-portable-9ca7e9c861775dd6c6312bc8aaab687403d24676
version=8.3-9ca7e9c
files="https://github.com/openssh/openssh-portable/archive/9ca7e9c861775dd6c6312bc8aaab687403d24676.tar.gz openssh-8.3-9ca7e9c.tar.gz 9ddfeabac11b59a1c5670a6905463ce2"
auth_type=md5
depends="zlib openssl"
useconfigure=true
configopts="--prefix=/usr/local --disable-utmp --disable-strip --sysconfdir=/etc/ssh --with-ssl-dir=${SERENITY_INSTALL_ROOT}/usr/local/lib"

pre_configure() {
    run autoreconf
}

install() {
    # Can't make keys outside of Serenity since ssh-keygen is built for Serenity.
    run make DESTDIR="${SERENITY_INSTALL_ROOT}" $installopts install-nokeys
}
