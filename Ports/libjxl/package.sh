#!/usr/bin/env -S bash ../.port_include.sh

port='libjxl'
version='0.13.0'
files=(
    "https://github.com/libjxl/libjxl/archive/refs/tags/v${version}.tar.gz#24d18c9af2d1f5a17730b3f8d3f70a5c16e5ac303f5d9d5fe9dc00cf7ca82af2"
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
