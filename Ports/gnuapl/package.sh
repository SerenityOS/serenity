#!/usr/bin/env -S bash ../.port_include.sh

port="gnuapl"
version="1.8"
useconfigure="true"
workdir="apl-${version}"
configopts=("CXX_WERROR=no")
files="https://ftpmirror.gnu.org/gnu/apl/apl-${version}.tar.gz apl-${version}.tar.gz https://ftpmirror.gnu.org/gnu/apl/apl-${version}.tar.gz.sig"
auth_type="md5"
