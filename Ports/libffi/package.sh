#!/usr/bin/env -S bash ../.port_include.sh
port='libffi'
version='3.8.1'
useconfigure='true'
files=(
    "https://github.com/libffi/libffi/releases/download/v${version}/libffi-${version}.tar.gz#81e879c094cc5ec3186c48510c6385ac6b3e866192e8bc71a35245703757ee40"
)
