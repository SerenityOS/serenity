#!/usr/bin/env -S bash ../.port_include.sh
port='openssh'
version='10.6p2'
files=(
    "https://cdn.openbsd.org/pub/OpenBSD/OpenSSH/portable/openssh-${version}.tar.gz#9ae44872090cf2f06b3cf9ff147e7f16f309671340763ed2f99248398ae29d5b"
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
