#!/usr/bin/env -S bash ../.port_include.sh
port=protobuf
useconfigure=true
version=24.3
depends=( "abseil" "zlib" )
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    # "-DCMAKE_CXX_STANDARD=14"
    "-Dprotobuf_BUILD_TESTS=OFF"
    # Don't use Protobuf's internal abseil, which is missing from the archive anyways.
    "-Dprotobuf_ABSL_PROVIDER=package"
)
files=(
    "https://github.com/protocolbuffers/protobuf/archive/refs/tags/v${version}.tar.gz#07d69502e58248927b58c7d7e7424135272ba5b2852a753ab6b67e62d2d29355"
)

configure() {
    run cmake -B build -G Ninja . "${configopts[@]}"
}

build() {
    run cmake --build build
}

install() {
    run cmake --install build
}
