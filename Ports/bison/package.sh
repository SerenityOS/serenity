#!/usr/bin/env -S bash ../.port_include.sh
port=bison
version=1.25
useconfigure=true
configopts=("--prefix=${SERENITY_INSTALL_ROOT}/usr/local")
files="https://ftpmirror.gnu.org/gnu/bison/bison-${version}.tar.gz bison-${version}.tar.gz 356bff0a058ca3d59528e0c49e68b90cdeb09779e0d626fc78a94270beed93a6"
auth_type=sha256
