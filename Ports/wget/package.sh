#!/usr/bin/env -S bash ../.port_include.sh
port=wget
version=1.21.1
useconfigure="true"
depends=("openssl")
files="https://ftpmirror.gnu.org/gnu/wget/wget-${version}.tar.gz wget-${version}.tar.gz
https://ftpmirror.gnu.org/gnu/wget/wget-${version}.tar.gz.sig wget-${version}.tar.gz.sig
https://ftpmirror.gnu.org/gnu/gnu-keyring.gpg gnu-keyring.gpg"
auth_type="sig"
auth_opts=("--keyring" "./gnu-keyring.gpg" "wget-${version}.tar.gz.sig")
configopts=("--with-ssl=openssl" "--disable-ipv6")

export OPENSSL_LIBS="-lssl -lcrypto -ldl"
