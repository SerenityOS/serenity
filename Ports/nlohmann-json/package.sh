#!/usr/bin/env -S bash ../.port_include.sh
port='nlohmann-json'
version='3.11.2'
workdir="json-${version}"
files=(
    "https://github.com/nlohmann/json/archive/refs/tags/v${version}.tar.gz#d69f9deb6a75e2580465c6c4c5111b89c4dc2fa94e3a85fcd2ffcd9a143d9273"
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
