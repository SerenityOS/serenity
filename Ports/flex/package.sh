#!/usr/bin/env -S bash ../.port_include.sh
port='flex'
version='2.6.14'
files=(
    "https://github.com/westes/flex/releases/download/v${version}/flex-${version}.tar.gz#3a5743b2cf171ab61843aed70b836a0a33a3fa70dd04f758f3a2b6a78e774680"
)
useconfigure='true'
configopts=(
    '--disable-bootstrap'
    'CFLAGS=-std=c17'
)
depends=('m4' 'pcre2')
