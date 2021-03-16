#!/bin/bash ../.port_include.sh
port=hatari
useconfigure=true
version=2.4.0-devel
depends="SDL2 zlib"
commit=353379e1f8a847cc0e284541d2b40fd49d175d22
workdir="${port}-${commit}"
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_ROOT/Toolchain/CMakeToolchain.txt"
files="https://github.com/hatari/hatari/archive/${commit}.tar.gz ${commit}.tar.gz"

configure() {
	run cmake $configopts
}

install() {
    run make install
}
