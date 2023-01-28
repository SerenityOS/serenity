#!/usr/bin/env -S bash ../.port_include.sh

port=gsl
description='GNU Scientific Library'
version=2.7.1
website='https://www.gnu.org/software/gsl/'
useconfigure=true
files="https://ftp.gnu.org/gnu/gsl/gsl-${version}.tar.gz gsl-${version}.tar.gz
https://ftp.gnu.org/gnu/gsl/gsl-${version}.tar.gz.sig gsl-${version}.tar.gz.sig
https://ftpmirror.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type=sig
auth_opts=("--keyring" "./gnu-keyring.gpg" "gsl-${version}.tar.gz.sig")
use_fresh_config_sub=true
