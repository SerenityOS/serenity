#!/usr/bin/env -S bash ../.port_include.sh
port=termcap
version=1.3.1
useconfigure=true
configopts="--prefix=${SERENITY_BUILD_DIR}/Root/usr/local"
files="https://ftpmirror.gnu.org/gnu/termcap/termcap-${version}.tar.gz termcap-${version}.tar.gz"
