#!/usr/bin/env -S bash ../.port_include.sh

port=flatbuffers
version=2.0.0
files=(
    "https://github.com/google/flatbuffers/archive/refs/tags/v${version}.tar.gz#9ddb9031798f4f8754d00fca2f1a68ecf9d0f83dfac7239af1311e4fd9a565c4"
)
useconfigure=true
# Since we are cross-compiling, we cannot build the tests, because we need
# the flatbuffers compiler to build them
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" "-DFLATBUFFERS_BUILD_TESTS=off")

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
