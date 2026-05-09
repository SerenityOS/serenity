#!/usr/bin/env -S bash ../.port_include.sh
port='msgpack-c'
version='6.1.0'
workdir='msgpack-c-c-6.1.0'
files=(
    "https://github.com/msgpack/msgpack-c/archive/refs/tags/c-${version}.tar.gz#c23c4070dbe01f46044bf70c5349f29453d655935b6dc710714c008bca0825a7"
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
