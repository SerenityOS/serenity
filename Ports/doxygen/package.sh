#!/usr/bin/env -S bash ../.port_include.sh
port='doxygen'
version='1.18.0'
files=(
    "https://github.com/doxygen/doxygen/archive/refs/tags/Release_${version//./_}.tar.gz#b32a3def78b0b75a2fd74ee6a63fb4a79cb6273fe31a570362e4e1871fa446da"
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
