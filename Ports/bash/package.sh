#!/usr/bin/env -S bash ../.port_include.sh
port=bash
version=5.1.16
useconfigure=true
use_fresh_config_sub=true
config_sub_path=support/config.sub
configopts=("--disable-nls" "--without-bash-malloc")
files="https://ftpmirror.gnu.org/gnu/bash/bash-${version}.tar.gz bash-${version}.tar.gz 5bac17218d3911834520dad13cd1f85ab944e1c09ae1aba55906be1f8192f558"
auth_type="sha256"

build() {
    run_replace_in_file "s/define GETCWD_BROKEN 1/undef GETCWD_BROKEN/" config.h
    run_replace_in_file "s/define CAN_REDEFINE_GETENV 1/undef CAN_REDEFINE_GETENV/" config.h
    run make "${makeopts[@]}"
}

post_install() {
    mkdir -p "${SERENITY_INSTALL_ROOT}/bin"
    ln -sf /usr/local/bin/bash "${SERENITY_INSTALL_ROOT}/bin/bash"
}
