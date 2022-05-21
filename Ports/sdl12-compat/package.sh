#!/usr/bin/env -S bash ../.port_include.sh
port=sdl12-compat
version=1.2.52
workdir=sdl12-compat-release-${version}
useconfigure=true
files="https://github.com/libsdl-org/sdl12-compat/archive/refs/tags/release-${version}.tar.gz ${port}-${version}.tar.gz 5bd7942703575554670a8767ae030f7921a0ac3c5e2fd173a537b7c7a8599014"
auth_type=sha256

configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt" "-DCMAKE_BUILD_TYPE=Release" "-DCMAKE_INSTALL_PREFIX=${SERENITY_INSTALL_ROOT}/usr/local/" "-B./build")
depends=("SDL2")

configure() {
    run cmake "${configopts[@]}"
}

build() {
    (
        cd ${workdir}/build/
        make "${makeopts[@]}"
    )
}

install() {
    (
        cd ${workdir}/build/
        make install
    )
}
