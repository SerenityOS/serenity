#!/bin/bash ../.port_include.sh
port=grep
version=2.5.4
files="https://ftp.gnu.org/gnu/grep/grep-${version}.tar.gz grep-${version}.tar.gz
https://ftp.gnu.org/gnu/grep/grep-${version}.tar.gz.sig grep-${version}.tar.gz.sig
https://ftp.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"

useconfigure=true
configopts=--disable-perl-regexp
auth_type="sig"
auth_opts="--keyring ./gnu-keyring.gpg grep-${version}.tar.gz.sig"