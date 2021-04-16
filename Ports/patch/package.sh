#!/usr/bin/env -S bash ../.port_include.sh
port=patch
version=6.6
files="https://github.com/ibara/libpuffy/releases/download/libpuffy-1.0/patch-${version}.tar.gz patch-${version}.tar.gz 451c14a5ec595465d0dcc05d86eed195"
auth_type=md5
depends=libpuffy
