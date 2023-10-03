#!/usr/bin/env -S bash ../.port_include.sh
port='protobuf'
useconfigure='true'
version='24.3'
depends=(
    'abseil'
    'zlib'
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-Dprotobuf_BUILD_TESTS=OFF'
    # Don't use Protobuf's internal abseil, which is missing from the archive anyways.
    '-Dprotobuf_ABSL_PROVIDER=package'
)
files=(
    "git+https://github.com/protocolbuffers/protobuf.git#v${version}"
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
