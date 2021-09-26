#!/usr/bin/env -S bash ../.port_include.sh
port=mpc
version=1.2.1
useconfigure=true
configopts=("--target=${SERENITY_ARCH}-pc-serenity" "--with-sysroot=/")
files="https://ftpmirror.gnu.org/gnu/mpc/mpc-${version}.tar.gz mpc-${version}.tar.gz
https://ftpmirror.gnu.org/gnu/mpc/mpc-${version}.tar.gz.sig mpc-${version}.tar.gz.sig
https://ftpmirror.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts=("--keyring" "./gnu-keyring.gpg" "mpc-${version}.tar.gz.sig")
depends=("gmp" "mpfr")
