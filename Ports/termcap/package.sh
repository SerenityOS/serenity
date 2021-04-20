#!/usr/bin/env -S bash ../.port_include.sh
port=termcap
version=1.3.1
useconfigure=true
configopts="--prefix=${SERENITY_INSTALL_ROOT}/usr/local"
files="https://ftpmirror.gnu.org/gnu/termcap/termcap-${version}.tar.gz termcap-${version}.tar.gz ffe6f86e63a3a29fa53ac645faaabdfa"
auth_type=md5
