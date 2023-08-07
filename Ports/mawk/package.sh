#!/usr/bin/env -S bash ../.port_include.sh
port=mawk
version=1.3.4-20200120
files=(
    "https://invisible-mirror.net/archives/mawk/mawk-${version}.tgz 7fd4cd1e1fae9290fe089171181bbc6291dfd9bca939ca804f0ddb851c8b8237"
)
useconfigure=true
use_fresh_config_sub=true

post_install() {
    ln -sf mawk "${SERENITY_INSTALL_ROOT}/usr/local/bin/awk"
}
