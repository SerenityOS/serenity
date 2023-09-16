#!/usr/bin/env -S bash ../.port_include.sh
port=abseil
useconfigure=true
version=20230802.0
depends=(  )
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DABSL_PROPAGATE_CXX_STD=ON"
    "-DABSL_ENABLE_INSTALL=ON"
    "-DABSL_BUILD_TESTING=OFF"
)
files=(
    "git+https://github.com/abseil/abseil-cpp.git#${version}"
)
workdir="abseil-cpp.git"

configure() {
    run cmake . "${configopts[@]}"
}

build() {
    run cmake --build . --target install
}
