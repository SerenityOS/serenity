#!/usr/bin/env -S bash ../.port_include.sh
port=libzip
useconfigure=true
version=1.7.3
depends="zlib"
workdir=libzip-${version}
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_ROOT/Toolchain/CMake/CMakeToolchain.txt"
files="https://libzip.org/download/libzip-${version}.tar.gz libzip-${version}.tar.gz 76f8fea9b88f6ead7f15ed7712eb5aef"
auth_type=md5

configure() {
    run cmake $configopts
}

install() {
    run make DESTDIR=$SERENITY_BUILD_DIR/Root install
}
