#!/usr/bin/env -S bash ../.port_include.sh

port='libjxl'
version='0.12.0'
files=(
    "https://github.com/libjxl/libjxl/archive/refs/tags/v${version}.tar.gz#03e9be69a30be4011f559da75328b6d7cea8ad921fabfbd551ce10bf45cdc992"
)
depends=(
    'brotli'
    'highway'
    'lcms2'
    'libpng'
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DCMAKE_BUILD_TYPE=Release"
    "-DBUILD_TESTING=OFF"
    "-DJPEGXL_ENABLE_BENCHMARK=OFF"
    "-DJPEGXL_ENABLE_SKCMS=OFF"
    "-DJPEGXL_ENABLE_SJPEG=OFF"
)
useconfigure='true'

configure() {
    run cmake "${configopts[@]}" .
}

install() {
    run make "${installopts[@]}" install
}
