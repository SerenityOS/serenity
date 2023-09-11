#!/usr/bin/env -S bash ../.port_include.sh

port='libjxl'
version='2023.09.11'
files=(
    "git+https://github.com/libjxl/libjxl#ff8a9c1cd54d324137c1905017542ffedeacdef8"
)

depends=(
    'brotli'
    'highway'
    'lcms2'
    'libpng'
)

workdir='libjxl'

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
