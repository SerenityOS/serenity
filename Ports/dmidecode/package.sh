#!/usr/bin/env -S bash ../.port_include.sh
port='dmidecode'
version='3.4'
useconfigure='false'
files="https://download.savannah.gnu.org/releases/dmidecode/dmidecode-${version}.tar.xz dmidecode-${version}.tar.xz 43cba851d8467c9979ccdbeab192eb6638c7d3a697eba5ddb779da8837542212"
auth_type='sha256'

install() {
    run make clean
    run make
}

post_install() {
    mkdir -p "${SERENITY_INSTALL_ROOT}/bin"
    run make install-bin DESTDIR="${SERENITY_INSTALL_ROOT}"
    ln -sf /usr/local/sbin/dmidecode "${SERENITY_INSTALL_ROOT}/bin/dmidecode"
    ln -sf /usr/local/sbin/biosdecode "${SERENITY_INSTALL_ROOT}/bin/biosdecode"
    ln -sf /usr/local/sbin/vpddecode "${SERENITY_INSTALL_ROOT}/bin/vpddecode"
}
