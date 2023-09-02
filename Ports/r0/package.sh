#!/usr/bin/env -S bash ../.port_include.sh
port=r0
version=0.8
useconfigure=false
workdir=$port-$version
files=(
    "https://github.com/radareorg/r0/archive/$version.tar.gz#a31722838a21cd3391c41bbb2e63f60552544244362b21f6d9a30d6c24c43bbe"
)
depends=()
