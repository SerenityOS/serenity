#!/bin/bash ../.port_include.sh
port=bash
version=5.0
useconfigure=true
configopts="--disable-nls --without-bash-malloc"
files="https://ftp.gnu.org/gnu/bash/bash-5.0.tar.gz bash-5.0.tar.gz"

build() {
    run_replace_in_file "s/define GETCWD_BROKEN 1/undef GETCWD_BROKEN/" config.h
    run_replace_in_file "s/define CAN_REDEFINE_GETENV 1/undef CAN_REDEFINE_GETENV/" config.h
    run make $makeopts
}
