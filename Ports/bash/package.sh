#!/usr/bin/env -S bash ../.port_include.sh
port='bash'
version='5.2.15'
useconfigure='true'
use_fresh_config_sub='true'
use_fresh_config_guess='true'
config_sub_paths=("support/config.sub")
config_guess_paths=("support/config.guess")
configopts=("--disable-nls" "--without-bash-malloc")
files=(
    "https://ftpmirror.gnu.org/gnu/bash/bash-${version}.tar.gz 13720965b5f4fc3a0d4b61dd37e7565c741da9a5be24edc2ae00182fc1b3588c"
)

build() {
    run_replace_in_file "s/define GETCWD_BROKEN 1/undef GETCWD_BROKEN/" config.h
    run_replace_in_file "s/define CAN_REDEFINE_GETENV 1/undef CAN_REDEFINE_GETENV/" config.h
    run make "${makeopts[@]}"
}

post_install() {
    mkdir -p "${SERENITY_INSTALL_ROOT}/bin"
    ln -sf /usr/local/bin/bash "${SERENITY_INSTALL_ROOT}/bin/bash"
}
