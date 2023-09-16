#!/usr/bin/env -S bash ../.port_include.sh
port='libsndfile'
version='1.2.2'
depends=(
    'flac'
    'lame'
    'libmpg123'
    'libogg'
    'libopus'
    'libvorbis'
    'sqlite'
)
useconfigure='true'
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_BUILD_TYPE=Release'
    '-DBUILD_TESTING=OFF'
    '-DBUILD_EXAMPLES=OFF'
    '-DENABLE_CPACK=OFF'
    '-DENABLE_STATIC_RUNTIME=OFF'
    '-DBUILD_SHARED_LIBS=ON'
)
files=(
    "https://github.com/libsndfile/libsndfile/archive/refs/tags/${version}.tar.gz#ffe12ef8add3eaca876f04087734e6e8e029350082f3251f565fa9da55b52121"
)

configure() {
    run cmake -G Ninja -B build -S . "${configopts[@]}"
}

build() {
    run cmake --build build
}

install() {
    run cmake --install build
}
