#!/usr/bin/env -S bash ../.port_include.sh

port="gnuapl"
version="1.8"
useconfigure="true"
workdir="apl-${version}"
configopts=("CXX_WERROR=no")
files=(
    "https://ftpmirror.gnu.org/gnu/apl/apl-${version}.tar.gz#144f4c858a0d430ce8f28be90a35920dd8e0951e56976cb80b55053fa0d8bbcb"
)
use_fresh_config_sub=true
