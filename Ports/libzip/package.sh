#!/usr/bin/env -S bash ../.port_include.sh
port='libzip'
version='1.11.4'
files=(
    "https://libzip.org/download/libzip-${version}.tar.gz#82e9f2f2421f9d7c2466bbc3173cd09595a88ea37db0d559a9d0a2dc60dc722e"
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
