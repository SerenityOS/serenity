#!/usr/bin/env -S bash ../.port_include.sh
port=curl
version=7.77.0
useconfigure=true
files="https://curl.se/download/curl-${version}.tar.bz2 curl-${version}.tar.bz2 6c0c28868cb82593859fc43b9c8fdb769314c855c05cf1b56b023acf855df8ea"
auth_type=sha256
depends="openssl zlib"
configopts="--disable-ntlm-wb --with-openssl=${SERENITY_INSTALL_ROOT}/usr/local"
