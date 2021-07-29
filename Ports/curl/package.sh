#!/usr/bin/env -S bash ../.port_include.sh
port=curl
version=7.78.0
useconfigure=true
files="https://curl.se/download/curl-${version}.tar.bz2 curl-${version}.tar.bz2 98530b317dc95ccb324bbe4f834f07bb642fbc393b794ddf3434f246a71ea44a"
auth_type=sha256
depends="openssl zlib"
configopts="--disable-ntlm-wb --with-openssl=${SERENITY_INSTALL_ROOT}/usr/local"
