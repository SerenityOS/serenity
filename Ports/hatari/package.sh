#!/usr/bin/env -S bash ../.port_include.sh
port=hatari
useconfigure=true
version=2.4.0-devel
depends="SDL2 zlib"
commit=353379e1f8a847cc0e284541d2b40fd49d175d22
workdir="${port}-${commit}"
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_SOURCE_DIR/Toolchain/CMake/CMakeToolchain.txt"
files="https://github.com/hatari/hatari/archive/${commit}.tar.gz ${commit}.tar.gz 614d8c20a06deea6df464a5de32cc795"
auth_type=md5

configure() {
	run cmake $configopts
}

install() {
    run make install
}
