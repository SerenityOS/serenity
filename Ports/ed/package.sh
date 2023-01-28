#!/usr/bin/env -S bash ../.port_include.sh
port=ed
description='GNU ed'
version=1.18
website='https://www.gnu.org/software/ed/'
files="https://ftpmirror.gnu.org/gnu/ed/ed-${version}.tar.lz ed-${version}.tar.lz
https://ftpmirror.gnu.org/gnu/ed/ed-${version}.tar.lz.sig ed-${version}.tar.lz.sig
https://ftpmirror.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts=("--keyring" "./gnu-keyring.gpg" "ed-${version}.tar.lz.sig")
useconfigure=true
depends=("pcre2")

configure() {
    run ./"$configscript"
}
