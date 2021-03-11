#!/usr/bin/env -S bash ../.port_include.sh
port=bison
version=1.25
useconfigure=true
configopts="--prefix=${SERENITY_BUILD_DIR}/Root/usr/local"
files="https://ftp.gnu.org/gnu/bison/bison-${version}.tar.gz bison-${version}.tar.gz 65f577d0f8ffaf61ae21c23c0918d225"
auth_type="md5"
