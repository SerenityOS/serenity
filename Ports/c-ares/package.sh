#!/usr/bin/env -S bash ../.port_include.sh
port='c-ares'
version='1.34.9'
files=(
    "https://github.com/c-ares/c-ares/releases/download/v${version}/c-ares-${version}.tar.gz#9364b70a12eb59a06830fb488d9357f90100dff0d98b23ea3593a183d43e6f2f"
)
useconfigure=true
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")

configure() {
    mkdir -p c-ares-build
    cmake -G Ninja \
        "${configopts[@]}" \
        -S "$workdir" \
        -B c-ares-build
}

build() {
    ninja -C c-ares-build "$makeopts"
}

install() {
    ninja -C c-ares-build install
}
