#!/usr/bin/env -S bash ../.port_include.sh
port='libzip'
version='1.10.1'
files=(
    "https://libzip.org/download/libzip-${version}.tar.gz#9669ae5dfe3ac5b3897536dc8466a874c8cf2c0e3b1fdd08d75b273884299363"
)
useconfigure='true'
depends=(
    'zlib'
)

configure() {
    run cmake \
        -DCMAKE_TOOLCHAIN_FILE="${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
}

install() {
    run make install
}
