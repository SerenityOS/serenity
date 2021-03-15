#!/bin/bash ../.port_include.sh
port=hatari
version=git
useconfigure=true
depends="SDL2 zlib"
workdir="${port}-${commit}"
commit=353379e1f8a847cc0e284541d2b40fd49d175d22
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_ROOT/Toolchain/CMakeToolchain.txt"
files="https://github.com/hatari/hatari/archive/${commit}.tar.gz ${commit}.tar.gz"

configure() {
	run cmake $configopts
}

install() {
    run make install
}

