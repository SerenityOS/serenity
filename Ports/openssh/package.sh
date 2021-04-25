#!/usr/bin/env -S bash ../.port_include.sh
port=openssh
workdir=openssh-portable-9ca7e9c861775dd6c6312bc8aaab687403d24676
version=8.3-9ca7e9c
files="https://github.com/openssh/openssh-portable/archive/9ca7e9c861775dd6c6312bc8aaab687403d24676.tar.gz openssh-8.3-9ca7e9c.tar.gz 78e3051cd76e505b1c9ea4fdcc108f47c64d4db058dad4f776908ed0229f6234"
auth_type=sha256
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
