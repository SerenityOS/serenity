#!/bin/bash ../.port_include.sh
port=libiconv
version=1.16
useconfigure=true
files="https://ftp.gnu.org/pub/gnu/libiconv/libiconv-${version}.tar.gz libiconv-${version}.tar.gz
http://ftp.gnu.org/gnu/libiconv/libiconv-${version}.tar.gz.sig libiconv-${version}.tar.gz.sig
https://ftp.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"

auth_type="sig"
auth_opts="--keyring ./gnu-keyring.gpg libiconv-${version}.tar.gz.sig"