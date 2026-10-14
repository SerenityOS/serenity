#!/usr/bin/env -S bash ../.port_include.sh
port='openssh'
version='10.6p1'
files=(
    "https://cdn.openbsd.org/pub/OpenBSD/OpenSSH/portable/openssh-${version}.tar.gz#a9dc9565dffe8640f64d863cd29a32bc4a3dbdec0566a7fc44c5d6ee767d5f39"
)
depends=(
    'openssl'
    'zlib'
)
useconfigure='true'
configopts=(
    '--prefix=/usr/local'
    '--sysconfdir=/etc/ssh'
    "--with-ssl-dir=${SERENITY_INSTALL_ROOT}/usr/local"
    '--disable-utmp'
    '--disable-utmpx'
    '--disable-wtmp'
    '--disable-wtmpx'
    '--disable-lastlog'
    '--disable-strip'
)

install() {
    # Can't make keys outside of Serenity since ssh-keygen is built for Serenity.
    run make DESTDIR="${SERENITY_INSTALL_ROOT}" "${installopts[@]}" install-nokeys
}
