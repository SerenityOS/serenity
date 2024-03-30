#!/usr/bin/env -S bash ../.port_include.sh
port=openssh
workdir=openssh-portable-94eb6858efecc1b4f02d8a6bd35e149f55c814c8
version=9.0-94eb685
files=(
    "https://github.com/openssh/openssh-portable/archive/94eb6858efecc1b4f02d8a6bd35e149f55c814c8.tar.gz#8a6bfb4c21d32f4e82d6d7734cd68585337cdd57428a2799295e1b1e72c332b5"
)
depends=("zlib" "openssl")
useconfigure=true
use_fresh_config_sub=true
configopts=("--prefix=/usr/local" "--disable-utmp" "--disable-strip" "--sysconfdir=/etc/ssh" "--with-ssl-dir=${SERENITY_INSTALL_ROOT}/usr/local/lib")

export LDFLAGS="-lcrypt -lcore -lcoreminimal"

pre_configure() {
    run autoreconf
}

install() {
    # Can't make keys outside of Serenity since ssh-keygen is built for Serenity.
    run make DESTDIR="${SERENITY_INSTALL_ROOT}" "${installopts[@]}" install-nokeys
}
