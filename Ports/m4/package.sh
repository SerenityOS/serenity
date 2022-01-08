#!/usr/bin/env -S bash ../.port_include.sh
port=m4
version=1.4.9
useconfigure=true
use_fresh_config_sub=true
files="https://ftpmirror.gnu.org/gnu/m4/m4-${version}.tar.gz m4-${version}.tar.gz
https://ftpmirror.gnu.org/gnu/m4/m4-${version}.tar.gz.sig m4-${version}.tar.gz.sig
https://ftpmirror.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts=("--keyring" "./gnu-keyring.gpg" "m4-${version}.tar.gz.sig")

# Stack overflow detection needs siginfo and sbrk, neither of which we support
export M4_cv_use_stackovf=no
