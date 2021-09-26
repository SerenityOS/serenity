#!/usr/bin/env -S bash ../.port_include.sh
port=patch
version=6.6
files="https://github.com/ibara/libpuffy/releases/download/libpuffy-1.0/patch-${version}.tar.gz patch-${version}.tar.gz b82ba726d9bdb683534839673f0c845d4f97c8d08490fa53dbef502665fee637"
auth_type=sha256
depends=("libpuffy")
