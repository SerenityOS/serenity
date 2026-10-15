#!/usr/bin/env -S bash ../.port_include.sh
port='libzip'
version='1.12.1'
files=(
    "https://libzip.org/download/libzip-${version}.tar.gz#3b66fe92cbc768d8d4d16e1427bfb7399e15d9c2795c2fc482e7b30a6332a422"
)
useconfigure='true'
depends=(
    'bzip2'
    'openssl'
    'xz'
    'zlib'
    'zstd'
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DBUILD_EXAMPLES=OFF'
    '-DBUILD_OSSFUZZ=OFF'
    '-DBUILD_DOC=OFF'
)

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
