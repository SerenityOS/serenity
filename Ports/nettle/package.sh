#!/usr/bin/env -S bash ../.port_include.sh
port='nettle'
version='3.9'
files=(
    "https://ftp.gnu.org/gnu/nettle/nettle-${version}.tar.gz 0ee7adf5a7201610bb7fe0acbb7c9b3be83be44904dd35ebbcd965cd896bfeaa"
)
useconfigure='true'
depends=(
    'gmp'
)
configopts=(
    '--with-included-libtasn1'
)
