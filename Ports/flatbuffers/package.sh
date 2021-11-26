#!/usr/bin/env -S bash ../.port_include.sh

port="flatbuffers"
version="1.12.0"
auth_type=sha256
files="https://github.com/google/flatbuffers/archive/refs/tags/v${version}.tar.gz v${version}.tar.gz 62f2223fb9181d1d6338451375628975775f7522185266cd5296571ac152bc45"
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
