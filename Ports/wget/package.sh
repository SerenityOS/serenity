#!/usr/bin/env -S bash ../.port_include.sh
port='wget'
version='1.24.5'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'build-aux/config.sub'
)
depends=(
    'openssl'
)
files=(
    "https://ftpmirror.gnu.org/gnu/wget/wget-${version}.tar.gz#fa2dc35bab5184ecbc46a9ef83def2aaaa3f4c9f3c97d4bd19dcb07d4da637de"
)
configopts=(
    '--with-ssl=openssl'
    '--disable-ipv6'
)

export OPENSSL_LIBS='-lssl -lcrypto -ldl'
