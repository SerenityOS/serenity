#!/usr/bin/env -S bash ../.port_include.sh
port=make
version=4.3
useconfigure=true
use_fresh_config_sub=true
config_sub_path=build-aux/config.sub
files="https://ftpmirror.gnu.org/gnu/make/make-${version}.tar.gz make-${version}.tar.gz
https://ftpmirror.gnu.org/gnu/make/make-${version}.tar.gz.sig make-${version}.tar.gz.sig
https://ftpmirror.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts=("--keyring" "./gnu-keyring.gpg" "make-${version}.tar.gz.sig")
configopts=("--target=${SERENITY_ARCH}-pc-serenity" "--with-sysroot=/" "--without-guile")
