#!/bin/bash ../.port_include.sh
port=bash
version=5.0
useconfigure=true
configopts="--disable-nls --without-bash-malloc"
files="https://ftp.gnu.org/gnu/bash/bash-${version}.tar.gz bash-${version}.tar.gz
https://ftp.gnu.org/gnu/bash/bash-${version}.tar.gz.sig bash-${version}.tar.gz.sig
https://ftp.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts="--keyring ./gnu-keyring.gpg bash-${version}.tar.gz.sig"

build() {
    run_replace_in_file "s/define GETCWD_BROKEN 1/undef GETCWD_BROKEN/" config.h
    run_replace_in_file "s/define CAN_REDEFINE_GETENV 1/undef CAN_REDEFINE_GETENV/" config.h
    run make $makeopts
}

post_install() {
    mkdir -p $SERENITY_ROOT/Build/Root/bin
    ln -s /usr/local/bin/bash $SERENITY_ROOT/Build/Root/bin/bash
}
