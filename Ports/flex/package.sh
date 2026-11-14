#!/usr/bin/env -S bash ../.port_include.sh
port='flex'
version='2.6.11'
files=(
    "https://github.com/westes/flex/releases/download/v${version}/flex-${version}.tar.gz#188e9c96b8fc032b2a0bdd24d4f0b692480c1854511c2fcc3034946f14fdf49a"
)
useconfigure='true'
configopts=(
    '--disable-bootstrap'
    'CFLAGS=-std=c17'
)
depends=('m4' 'pcre2')
