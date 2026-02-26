#!/usr/bin/env -S bash ../.port_include.sh

port=flatbuffers
version=25.12.19
files=(
    "https://github.com/google/flatbuffers/archive/refs/tags/v${version}.tar.gz#f81c3162b1046fe8b84b9a0dbdd383e24fdbcf88583b9cb6028f90d04d90696a"
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
