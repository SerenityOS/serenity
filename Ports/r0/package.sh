#!/usr/bin/env -S bash ../.port_include.sh
port=r0
version=0.8
useconfigure=false
workdir=$port-$version
files="https://codeload.github.com/radareorg/r0/tar.gz/refs/tags/$version r0-$version.tar.gz a31722838a21cd3391c41bbb2e63f60552544244362b21f6d9a30d6c24c43bbe"
auth_type=sha256
depends=()
