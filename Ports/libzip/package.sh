#!/usr/bin/env -S bash ../.port_include.sh
port='libzip'
version='1.12'
files=(
    "https://libzip.org/download/libzip-${version}.tar.gz#03db61e150b136b1692a9953f9fdab148808c4e504c1b7c5acad8ec7a50321cb"
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
