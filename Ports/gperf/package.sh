#!/usr/bin/env -S bash ../.port_include.sh
port=gperf
version=3.1
useconfigure="true"
depends=()
files=(
    "https://ftpmirror.gnu.org/gnu/gperf/gperf-${version}.tar.gz#588546b945bba4b70b6a3a616e80b4ab466e3f33024a352fc2198112cdbb3ae2"
)
configopts=("--prefix=/usr/local")
