#!/usr/bin/env -S bash ../.port_include.sh
port='libzip'
version='1.8.0'
files=(
    "https://libzip.org/download/libzip-${version}.tar.gz libzip-${version}.tar.gz 30ee55868c0a698d3c600492f2bea4eb62c53849bcf696d21af5eb65f3f3839e"
)
useconfigure='true'
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
depends=("zlib")
workdir="libzip-${version}"

configure() {
    run cmake "${configopts[@]}"
}

install() {
    run make install
}
