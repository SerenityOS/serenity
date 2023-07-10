#!/usr/bin/env -S bash ../.port_include.sh
port=gmp
version=6.2.1
useconfigure="true"
files=(
    "https://ftpmirror.gnu.org/gnu/gmp/gmp-${version}.tar.bz2 gmp-${version}.tar.bz2 eae9326beb4158c386e39a356818031bd28f3124cf915f8c5b1dc4c7a36b4d7c"
)
