#!/usr/bin/env -S bash ../.port_include.sh
port='mbedtls'
version='4.2.0'
useconfigure='true'
files=(
    "https://github.com/Mbed-TLS/mbedtls/releases/download/mbedtls-${version}/mbedtls-${version}.tar.bz2#2bed9d713b4668f76553b097e72b8aa30bc8f112a940d7ae228d524bbde6ffea"
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_BUILD_TYPE=Release'
    '-DUSE_SHARED_MBEDTLS_LIBRARY=ON'
    '-DENABLE_PROGRAMS=OFF'
    '-DENABLE_TESTING=OFF'
    '-GNinja'
)

configure() {
    run cmake "${configopts[@]}" .
}

build() {
    run ninja
}

install() {
    run ninja install
}
