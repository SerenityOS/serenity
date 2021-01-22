#!/usr/bin/env -S bash ../.port_include.sh
port=make
version=4.3
useconfigure=true
files="https://ftp.gnu.org/gnu/make/make-${version}.tar.gz make-${version}.tar.gz
http://ftp.gnu.org/gnu/make/make-${version}.tar.gz.sig make-${version}.tar.gz.sig
https://ftp.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts="--keyring ./gnu-keyring.gpg make-${version}.tar.gz.sig"
configopts="--target=i686-pc-serenity --with-sysroot=/ --without-guile"
