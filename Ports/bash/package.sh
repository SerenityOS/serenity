#!/usr/bin/env -S bash ../.port_include.sh
port='bash'
version='5.2.37'
useconfigure='true'
use_fresh_config_sub='true'
use_fresh_config_guess='true'
config_sub_paths=("support/config.sub")
config_guess_paths=("support/config.guess")
configopts=("--disable-nls" "--without-bash-malloc" "CFLAGS=-std=c17" "CFLAGS_FOR_BUILD=-std=c17")
launcher_name='Bash'
launcher_category='&Utilities'
launcher_command='/usr/local/bin/bash'
launcher_run_in_terminal='true'
#icon_file=FIXME
files=(
    "https://ftpmirror.gnu.org/gnu/bash/bash-${version}.tar.gz#9599b22ecd1d5787ad7d3b7bf0c59f312b3396d1e281175dd1f8a4014da621ff"
)
depends=(
    'readline'
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
