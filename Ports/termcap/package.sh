#!/usr/bin/env -S bash ../.port_include.sh
port=termcap
version=1.3.1
useconfigure=true
configopts=("--prefix=${SERENITY_INSTALL_ROOT}/usr/local")
files=(
    "https://ftpmirror.gnu.org/gnu/termcap/termcap-${version}.tar.gz 91a0e22e5387ca4467b5bcb18edf1c51b930262fd466d5fda396dd9d26719100"
)
