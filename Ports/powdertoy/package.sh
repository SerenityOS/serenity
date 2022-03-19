#!/usr/bin/env -S bash ../.port_include.sh
port=powdertoy
version=96.2.350
useconfigure=true
configopts=("--cross-file" "../cross_file-$SERENITY_ARCH.txt")
depends=("luajit" "curl" "libfftw3f" "zlib" "SDL2")
files="https://github.com/The-Powder-Toy/The-Powder-Toy/archive/refs/tags/v${version}.tar.gz The-Powder-Toy-${version}.tar.gz d95cbadee22632687661e8fc488bd64405d81c0dca737e16420f26e93ea5bf58"
auth_type=sha256
workdir="The-Powder-Toy-${version}"
launcher_name="The Powder Toy"
launcher_category="Games"
launcher_command="/usr/local/bin/powder"
launcher_run_in_terminal=false

configure() {
    run meson build-debug "${configopts[@]}"
}

build() {
    run ninja -C build-debug
}

install() {
    run cp build-debug/powder "${SERENITY_INSTALL_ROOT}/usr/local/bin"
}
