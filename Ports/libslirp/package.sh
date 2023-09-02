#!/usr/bin/env -S bash ../.port_include.sh
port='libslirp'
version='4.7.0'
workdir="libslirp-v${version}"
files=(
    "https://gitlab.freedesktop.org/slirp/libslirp/-/archive/v${version}/libslirp-v${version}.tar.gz#9398f0ec5a581d4e1cd6856b88ae83927e458d643788c3391a39e61b75db3d3b"
)
useconfigure='true'
configopts=("--cross-file=${SERENITY_BUILD_DIR}/meson-cross-file.txt")
depends=('glib')

configure() {
    run meson _build "${configopts[@]}"
}

build() {
    run ninja -C _build
}

install() {
    export DESTDIR="${SERENITY_INSTALL_ROOT}"
    run meson install -C _build
}
