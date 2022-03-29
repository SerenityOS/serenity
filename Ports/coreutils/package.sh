#!/usr/bin/env -S bash ../.port_include.sh
port=coreutils
version=9.0
useconfigure="true"
use_fresh_config_sub="true"
config_sub_path=build-aux/config.sub
files="https://ftpmirror.gnu.org/gnu/coreutils/coreutils-${version}.tar.gz coreutils-${version}.tar.gz
https://ftpmirror.gnu.org/gnu/coreutils/coreutils-${version}.tar.gz.sig coreutils-${version}.tar.gz.sig
https://ftpmirror.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts=("--keyring" "./gnu-keyring.gpg" "coreutils-${version}.tar.gz.sig")
