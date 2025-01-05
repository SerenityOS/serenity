#!/usr/bin/env -S bash ../.port_include.sh
port='mawk'
version='1.3.4-20240905'
files=(
    "https://invisible-mirror.net/archives/mawk/mawk-${version}.tgz#a39967927dfa1b0116efc45b944a0f5b5b4c34f8e842a4b223dcdd7b367399e0"
)
useconfigure='true'
use_fresh_config_sub='true'

post_install() {
    ln -sf mawk "${SERENITY_INSTALL_ROOT}/usr/local/bin/awk"
}
