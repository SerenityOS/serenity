#!/usr/bin/env -S bash ../.port_include.sh
port=powdertoy
version=96.2.350
useconfigure=true
configopts=("-Dbuildtype=release" "build-release" "--cross-file" "${SERENITY_BUILD_DIR}/meson-cross-file.txt")
depends=("luajit" "curl" "libfftw3f" "zlib" "SDL2")
files=(
    "https://github.com/The-Powder-Toy/The-Powder-Toy/archive/refs/tags/v${version}.tar.gz#d95cbadee22632687661e8fc488bd64405d81c0dca737e16420f26e93ea5bf58"
)
workdir="The-Powder-Toy-${version}"
launcher_name="The Powder Toy"
icon_file="resources/icon.ico"
launcher_category="&Games"
launcher_command="/usr/local/bin/powder"
launcher_run_in_terminal=false

configure() {
    run meson "${configopts[@]}"
}

build() {
    run ninja -C build-release
}

install() {
    run cp build-release/powder "${SERENITY_INSTALL_ROOT}/usr/local/bin"
}
