#!/usr/bin/env -S bash ../.port_include.sh
port='nlohmann-json'
version='3.12.0'
workdir="json-${version}"
files=(
    "https://github.com/nlohmann/json/archive/refs/tags/v${version}.tar.gz#4b92eb0c06d10683f7447ce9406cb97cd4b453be18d7279320f7b2f025c10187"
)
useconfigure='true'
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DCMAKE_INSTALL_DATADIR=${SERENITY_INSTALL_ROOT}/usr/local/lib"
    "-DJSON_BuildTests=OFF"
)

configure() {
    mkdir -p "${PORT_BUILD_DIR}/json-${version}-build"
    cd "${PORT_BUILD_DIR}/json-${version}-build"
    cmake "${configopts[@]}" "${PORT_BUILD_DIR}/json-${version}"
}

build() {
    cd "${PORT_BUILD_DIR}/json-${version}-build"
    make "${makeopts[@]}"
}

install() {
    cd "${PORT_BUILD_DIR}/json-${version}-build"
    make install
}
