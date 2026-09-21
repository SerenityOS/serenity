#!/usr/bin/env -S bash ../.port_include.sh
port='msgpack-c'
version='7.0.2'
workdir="msgpack-c-c-${version}"
files=(
    "https://github.com/msgpack/msgpack-c/archive/refs/tags/c-${version}.tar.gz#f720af974b72cda874c2c347d4bafef5385c4e7942a27b3c87d6fa482412d08c"
)
useconfigure='true'
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_BUILD_TYPE=Release'
    '-DBUILD_SHARED_LIBS=ON'
    '-DMSGPACK_ENABLE_STATIC=OFF'
    '-DMSGPACK_BUILD_EXAMPLES=OFF'
)

configure() {
    run cmake -G Ninja -B build -S . "${configopts[@]}"
}

build() {
    run cmake --build build --parallel "${MAKEJOBS}"
}

install() {
    run cmake --install build
}
