#!/usr/bin/env -S bash ../.port_include.sh
port=gawk
version=5.1.1
useconfigure="true"
files="https://ftpmirror.gnu.org/gnu/gawk/gawk-${version}.tar.gz gawk-${version}.tar.gz
https://ftpmirror.gnu.org/gnu/gawk/gawk-${version}.tar.gz.sig gawk-${version}.tar.gz.sig
https://ftpmirror.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts=("--keyring" "./gnu-keyring.gpg" "gawk-${version}.tar.gz.sig")
