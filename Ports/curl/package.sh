#!/usr/bin/env -S bash ../.port_include.sh
port=curl
version=7.77.0
useconfigure=true
configopts="--disable-ntlm-wb --with-ssl"
files="https://curl.se/download/curl-${version}.tar.bz2 curl-${version}.tar.bz2
https://curl.se/download/curl-${version}.tar.bz2.asc curl-${version}.tar.bz2.asc"

depends="zlib openssl"
auth_type="sig"
auth_import_key="27EDEAF22F3ABCEB50DB9A125CC908FDB71E12C2"
auth_opts="curl-${version}.tar.bz2.asc curl-${version}.tar.bz2"
