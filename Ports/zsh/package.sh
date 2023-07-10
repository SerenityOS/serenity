#!/usr/bin/env -S bash ../.port_include.sh
port=zsh
version=5.9
files=(
    "https://sourceforge.net/projects/zsh/files/zsh/${version}/zsh-${version}.tar.xz zsh-${version}.tar.xz 9b8d1ecedd5b5e81fbf1918e876752a7dd948e05c1a0dba10ab863842d45acd5"
)
useconfigure=true
use_fresh_config_sub=true

pre_configure() {
    run "./Util/preconfig"
}

post_configure() {
    run_replace_in_file "s/define HAVE_PRCTL 1/undef HAVE_PRCTL/" config.h
}

post_install() {
    cp "${PORT_META_DIR}/zshrc" "${SERENITY_INSTALL_ROOT}/etc/"
}
