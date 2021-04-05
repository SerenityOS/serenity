#!/usr/bin/env -S bash ../.port_include.sh
port=libzip
useconfigure=true
version=1.7.3
depends="zlib"
workdir=libzip-${version}
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_ROOT/Toolchain/CMakeToolchain.txt"
files="https://libzip.org/download/libzip-${version}.tar.gz libzip-${version}.tar.gz"

configure() {
    run cmake $configopts
}

install() {
    run make DESTDIR=$SERENITY_BUILD_DIR/Root install
}
