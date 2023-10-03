#!/usr/bin/env -S bash ../.port_include.sh
port='abseil'
useconfigure='true'
version='20230802.0'
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DABSL_PROPAGATE_CXX_STD=ON'
    '-DABSL_ENABLE_INSTALL=ON'
    '-DABSL_BUILD_TESTING=OFF'
)
files=(
    "git+https://github.com/abseil/abseil-cpp.git#${version}"
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
