#!/usr/bin/env -S bash ../.port_include.sh
port='protobuf'
useconfigure='true'
version='36.2'
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
    "https://github.com/protocolbuffers/protobuf/releases/download/v${version}/protobuf-${version}.tar.gz#3d9642a662d10e68ebae5e53f14dcce5105684212d5078f8e0d47d1ab3ae6b64"
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
