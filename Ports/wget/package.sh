#!/usr/bin/env -S bash ../.port_include.sh
port='wget'
version='1.25.0'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'build-aux/config.sub'
)
depends=(
    'openssl'
)
files=(
    "https://ftpmirror.gnu.org/gnu/wget/wget-${version}.tar.gz#766e48423e79359ea31e41db9e5c289675947a7fcf2efdcedb726ac9d0da3784"
)
configopts=(
    '--with-ssl=openssl'
    '--disable-ipv6'
)

export OPENSSL_LIBS='-lssl -lcrypto -ldl'
