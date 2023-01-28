#!/usr/bin/env -S bash ../.port_include.sh
port=gmp
description='GNU Multiple Precision Arithmetic Library (GMP)'
version=6.2.1
website='https://gmplib.org/'
useconfigure="true"
files="https://ftpmirror.gnu.org/gnu/gmp/gmp-${version}.tar.bz2 gmp-${version}.tar.bz2
https://ftpmirror.gnu.org/gnu/gmp/gmp-${version}.tar.bz2.sig gmp-${version}.tar.bz2.sig
https://ftpmirror.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts=("--keyring" "./gnu-keyring.gpg" "gmp-${version}.tar.bz2.sig")
