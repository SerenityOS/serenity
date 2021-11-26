#!/usr/bin/env -S bash ../.port_include.sh
port=bash
version=5.1.8
useconfigure=true
configopts=("--disable-nls" "--without-bash-malloc")
files="https://ftpmirror.gnu.org/gnu/bash/bash-${version}.tar.gz bash-${version}.tar.gz 0cfb5c9bb1a29f800a97bd242d19511c997a1013815b805e0fdd32214113d6be"
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
