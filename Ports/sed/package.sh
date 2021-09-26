#!/usr/bin/env -S bash ../.port_include.sh
port=sed
version=4.2.1
useconfigure="true"
files="https://ftpmirror.gnu.org/gnu/sed/sed-${version}.tar.bz2 sed-${version}.tar.bz2
https://ftpmirror.gnu.org/gnu/sed/sed-${version}.tar.bz2.sig sed-${version}.tar.bz2.sig
https://ftpmirror.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts=("--keyring" "./gnu-keyring.gpg" "sed-${version}.tar.bz2.sig")
