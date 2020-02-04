#!/bin/bash ../.port_include.sh
port=diffutils
version=3.5
files="https://ftp.gnu.org/gnu/diffutils/diffutils-${version}.tar.xz diffutils-${version}.tar.xz
https://ftp.gnu.org/gnu/diffutils/diffutils-${version}.tar.xz.sig diffutils-${version}.tar.xz.sig
https://ftp.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
useconfigure=true
auth_type="sig"
auth_opts="--keyring ./gnu-keyring.gpg diffutils-${version}.tar.xz.sig"
