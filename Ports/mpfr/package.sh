#!/usr/bin/env -S bash ../.port_include.sh
port=mpfr
version=4.1.0
useconfigure=true
configopts=("--target=${SERENITY_ARCH}-pc-serenity" "--with-sysroot=/")
files="https://ftpmirror.gnu.org/gnu/mpfr/mpfr-${version}.tar.xz mpfr-${version}.tar.xz
https://ftpmirror.gnu.org/gnu/mpfr/mpfr-${version}.tar.xz.sig mpfr-${version}.tar.xz.sig
https://ftpmirror.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts=("--keyring" "./gnu-keyring.gpg" "mpfr-${version}.tar.xz.sig")
depends=("gmp")
