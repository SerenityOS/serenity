#!/usr/bin/env -S bash ../.port_include.sh
port='libslirp'
version='4.9.7'
workdir="libslirp-v${version}"
files=(
    "https://gitlab.freedesktop.org/slirp/libslirp/-/archive/v${version}/libslirp-v${version}.tar.gz#d2567fa06a6357c7755d61c67293b2c0bf069b3f2173bde6b76cf768529a1938"
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
