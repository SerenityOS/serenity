#!/bin/bash ../.toolchain_include.sh

package=libstdc++-v3
version=9.2.0

makeopts="all-target-libstdc++-v3"
installopts="install-target-libstdc++-v3"
depends="serenity-libc serenity-libm gcc"
sourcedir=../gcc/gcc-${version}

fetch() {
    echo ""
}

