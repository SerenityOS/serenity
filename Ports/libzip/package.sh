#!/usr/bin/env -S bash ../.port_include.sh
port=libzip
useconfigure=true
version=1.7.3
depends=("zlib")
workdir=libzip-${version}
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
files="https://libzip.org/download/libzip-${version}.tar.gz libzip-${version}.tar.gz 0e2276c550c5a310d4ebf3a2c3dfc43fb3b4602a072ff625842ad4f3238cb9cc"
auth_type=sha256

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make DESTDIR=$SERENITY_BUILD_DIR/Root install
}
