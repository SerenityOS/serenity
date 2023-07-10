#!/usr/bin/env -S bash ../.port_include.sh
port='grep'
version='3.10'
files=(
    "https://ftpmirror.gnu.org/gnu/grep/grep-${version}.tar.gz grep-${version}.tar.gz de7b21d8e3348ea6569c6fd5734e90a31169ef62429ea3dce48a6fc1dd85d260"
)
useconfigure='true'
configopts=("--disable-perl-regexp")
