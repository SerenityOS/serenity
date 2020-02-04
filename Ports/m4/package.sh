#!/bin/bash ../.port_include.sh
port=m4
version=1.4.9
useconfigure=true
files="http://ftp.gnu.org/gnu/m4/m4-${version}.tar.gz m4-${version}.tar.gz
http://ftp.gnu.org/gnu/m4/m4-${version}.tar.gz.sig m4-${version}.tar.gz.sig
https://ftp.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts="--keyring ./gnu-keyring.gpg m4-${version}.tar.gz.sig"