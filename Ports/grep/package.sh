#!/usr/bin/env -S bash ../.port_include.sh
port='grep'
version='3.12'
files=(
    "https://ftpmirror.gnu.org/gnu/grep/grep-${version}.tar.gz#badda546dfc4b9d97e992e2c35f3b5c7f20522ffcbe2f01ba1e9cdcbe7644cdc"
)
useconfigure='true'
configopts=(
    '--disable-perl-regexp'
)
