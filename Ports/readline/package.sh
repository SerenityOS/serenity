#!/usr/bin/env -S bash ../.port_include.sh

port=readline
version=8.1
useconfigure=true
files="https://ftpmirror.gnu.org/gnu/readline/readline-${version}.tar.gz readline-${version}.tar.gz
https://ftpmirror.gnu.org/gnu/readline/readline-${version}.tar.gz.sig readline-${version}.tar.gz.sig
https://ftpmirror.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts="--keyring ./gnu-keyring.gpg readline-${version}.tar.gz.sig"
