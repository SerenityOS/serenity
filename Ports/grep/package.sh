#!/usr/bin/env -S bash ../.port_include.sh
port=grep
description='GNU Grep'
version=3.8
website='https://www.gnu.org/software/grep/'
files="https://ftpmirror.gnu.org/gnu/grep/grep-${version}.tar.gz grep-${version}.tar.gz
https://ftpmirror.gnu.org/gnu/grep/grep-${version}.tar.gz.sig grep-${version}.tar.gz.sig
https://ftpmirror.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"

useconfigure=true
configopts=("--disable-perl-regexp")
auth_type="sig"
auth_opts=("--keyring" "./gnu-keyring.gpg" "grep-${version}.tar.gz.sig")
