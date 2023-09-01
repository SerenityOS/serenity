#!/usr/bin/env -S bash ../.port_include.sh
port='mawk'
version='1.3.4-20230808'
files=(
    "https://invisible-mirror.net/archives/mawk/mawk-${version}.tgz#88f55a632e2736ff5c5f69944abc151734d89d8298d5005921180f39ab7ba6d0"
)
useconfigure='true'
use_fresh_config_sub='true'

post_install() {
    ln -sf mawk "${SERENITY_INSTALL_ROOT}/usr/local/bin/awk"
}
