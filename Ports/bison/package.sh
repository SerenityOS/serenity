#!/usr/bin/env -S bash ../.port_include.sh
port=bison
version=1.25
useconfigure=true
configopts="--prefix=${SERENITY_INSTALL_ROOT}/usr/local"
files="https://ftpmirror.gnu.org/gnu/bison/bison-${version}.tar.gz bison-${version}.tar.gz 65f577d0f8ffaf61ae21c23c0918d225"
auth_type="md5"
