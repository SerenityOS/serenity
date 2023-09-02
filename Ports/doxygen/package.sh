#!/usr/bin/env -S bash ../.port_include.sh
port='doxygen'
version='1.9.7'
files=(
    "https://github.com/doxygen/doxygen/archive/refs/tags/Release_${version//./_}.tar.gz#691777992a7240ed1f822a5c2ff2c4273b57c1cf9fc143553d87f91a0c5970ee"
)
workdir="${port}-Release_${version//./_}"
useconfigure='true'
configopts=(
    '-Bbuild'
    '-GNinja'
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_BUILD_TYPE=Release'
    '-DCMAKE_POLICY_DEFAULT_CMP0148=OLD'
)
depends=(
    'libiconv'
)

configure() {
    run cmake "${configopts[@]}"
}

build() {
    run cmake --build build -j "$MAKEJOBS"
}

install() {
    run cmake --install build --prefix "$SERENITY_INSTALL_ROOT"
}
