#!/usr/bin/env -S bash ../.port_include.sh
port='links'
version='2.30'
useconfigure='true'
files=(
    "http://links.twibright.com/download/links-${version}.tar.bz2#c4631c6b5a11527cdc3cb7872fc23b7f2b25c2b021d596be410dadb40315f166"
)
depends=(
    'openssl'
)
