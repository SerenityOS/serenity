#!/usr/bin/env -S bash ../.port_include.sh
port='wget'
version='1.21.4'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'build-aux/config.sub'
)
depends=(
    'openssl'
)
files=(
    "https://ftpmirror.gnu.org/gnu/wget/wget-${version}.tar.gz#81542f5cefb8faacc39bbbc6c82ded80e3e4a88505ae72ea51df27525bcde04c"
)
configopts=(
    '--with-ssl=openssl'
    '--disable-ipv6'
)

export OPENSSL_LIBS='-lssl -lcrypto -ldl'
