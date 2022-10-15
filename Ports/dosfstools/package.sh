#!/usr/bin/env -S bash ../.port_include.sh
port='dosfstools'
version='4.2'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('config.sub')
files="https://github.com/dosfstools/dosfstools/releases/download/v${version}/dosfstools-${version}.tar.gz dosfstools-${version}.tar.gz 64926eebf90092dca21b14259a5301b7b98e7b1943e8a201c7d726084809b527"
auth_type='sha256'

post_install() {
    ln -sf /usr/local/sbin/mkfs.fat "${SERENITY_INSTALL_ROOT}/usr/local/sbin/mkfs.vfat"
    ln -sf /usr/local/sbin/mkfs.fat "${SERENITY_INSTALL_ROOT}/usr/local/sbin/mkfs.msdos"
}
