#!/usr/bin/env -S bash ../.port_include.sh
port=luajit
version=18b087cd2cd4ddc4a79782bf155383a689d5093d
useconfigure=true
files=(
    "git+https://luajit.org/git/luajit.git#${version}"
)
workdir="LuaJIT-${version}"

if [ ${SERENITY_ARCH} = "x86_64" ]; then
    M_FLAG=-m64
fi

makeopts=("CROSS=${SERENITY_ARCH}-serenity-" "HOST_CC=${HOST_CC} ${M_FLAG}")

configure() {
    printf "\x1b[31m\x1b[5mATTENTION: \x1b[0m\x1b[31m\x1b[1mIf this fails, install either libc6-dev-i386 or libc6-dev-amd64\x1b[0m\n"
}
