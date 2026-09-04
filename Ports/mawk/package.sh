#!/usr/bin/env -S bash ../.port_include.sh
port='mawk'
version='1.3.4-20260302'
files=(
    "https://invisible-mirror.net/archives/mawk/mawk-${version}.tgz#e2c08a77d0a84a01f9be454d1ca3872d4f103f9ada683d075198b0c6e965633d"
)
useconfigure='true'
use_fresh_config_sub='true'

post_install() {
    ln -sf mawk "${SERENITY_INSTALL_ROOT}/usr/local/bin/awk"
}
