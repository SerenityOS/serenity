#!/usr/bin/env -S bash ../.port_include.sh
port='grep'
version='3.11'
files=(
    "https://ftpmirror.gnu.org/gnu/grep/grep-${version}.tar.gz#1f31014953e71c3cddcedb97692ad7620cb9d6d04fbdc19e0d8dd836f87622bb"
)
useconfigure='true'
configopts=(
    '--disable-perl-regexp'
)
