#!/bin/bash ../.port_include.sh
port=ninja
version=1.8.2
workdir=ninja-1.8.2
files="https://github.com/ninja-build/ninja/archive/v1.8.2.tar.gz ninja-v1.8.2.tar.gz"

build() {
    CXX=i686-pc-serenity-g++ CXXFLAGS="--sysroot=${SERENITY_ROOT}/Build/Root" \
    LDFLAGS="--sysroot=${SERENITY_ROOT}/Build/Root" \
    # platform=linux is close enough.
    run ./configure.py --bootstrap --platform=linux
    strip "${workdir}/ninja"
}

install() {
    mkdir -p "${SERENITY_ROOT}/Build/Root/usr/local/bin"
    cp "${workdir}/ninja" "${SERENITY_ROOT}/Build/Root/usr/local/bin/ninja"
}
