#!/usr/bin/env -S bash ../.port_include.sh
port='abseil'
useconfigure='true'
version='20260817.0'
workdir="abseil-cpp-${version}"
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DABSL_PROPAGATE_CXX_STD=ON'
    '-DABSL_ENABLE_INSTALL=ON'
    '-DABSL_BUILD_TESTING=OFF'
)
files=(
    "https://github.com/abseil/abseil-cpp/releases/download/${version}/abseil-cpp-${version}.tar.gz#f7e05179df39c45434cad433f5783840bb3788ef322976f9138bc6b72b3a107d"
)

configure() {
    run cmake . "${configopts[@]}"
}

build() {
    run cmake --build .
}

install() {
    run cmake --build . --target install
}
