#!/usr/bin/env -S bash ../.port_include.sh
port=sed
version=4.9
useconfigure="true"
use_fresh_config_sub="true"
config_sub_paths=("build-aux/config.sub")
files="https://ftpmirror.gnu.org/gnu/sed/sed-${version}.tar.gz sed-${version}.tar.gz
https://ftpmirror.gnu.org/gnu/sed/sed-${version}.tar.gz.sig sed-${version}.tar.gz.sig
https://ftpmirror.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts=("--keyring" "./gnu-keyring.gpg" "sed-${version}.tar.gz.sig")
