#!/usr/bin/env -S bash ../.port_include.sh
port=luajit
version=2.1.0-beta3
files="https://luajit.org/download/LuaJIT-${version}.tar.gz LuaJIT-${version}.tar.gz 1ad2e34b111c802f9d0cdf019e986909123237a28c746b21295b63c9e785d9c3"
auth_type=sha256
workdir="LuaJIT-${version}"

preconfigure() {
    printf "\x1b[31m\x1b[5mATTENTION: \x1b[0m\x1b[31m\x1b[1mIf this fails, install either libc6-dev-i386 or libc6-dev-amd64\x1b[0m\n"
}

if [ ${SERENITY_ARCH} = "i686" ]; then
    M_FLAG=-m32
elif [ ${SERENITY_ARCH} = "x86_64" ]; then
    M_FLAG=-m64
fi

makeopts=("CROSS=${SERENITY_ARCH}-pc-serenity-" "HOST_CC=gcc ${M_FLAG}")
