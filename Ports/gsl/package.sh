#!/usr/bin/env -S bash ../.port_include.sh

port="gsl"
version="2.7"
useconfigure="true"
files="https://mirror.freedif.org/GNU/gsl/gsl-${version}.tar.gz gsl-${version}.tar.gz
https://mirror.freedif.org/GNU/gsl/gsl-${version}.tar.gz.sig gsl-${version}.tar.gz.sig
https://ftpmirror.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type=sig
auth_opts=("--keyring" "./gnu-keyring.gpg" "gsl-${version}.tar.gz.sig")
