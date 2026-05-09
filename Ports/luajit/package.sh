#!/usr/bin/env -S bash ../.port_include.sh
port=luajit
version=18b087cd2cd4ddc4a79782bf155383a689d5093d
files=(
    "git+https://luajit.org/git/luajit.git#${version}"
)
workdir="LuaJIT-${version}"

makeopts=("CROSS=${SERENITY_ARCH}-serenity-" "HOST_CC=${HOST_CC}")
