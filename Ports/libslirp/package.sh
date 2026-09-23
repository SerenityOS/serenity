#!/usr/bin/env -S bash ../.port_include.sh
port='libslirp'
version='4.9.5'
workdir="libslirp-v${version}"
files=(
    "https://gitlab.freedesktop.org/slirp/libslirp/-/archive/v${version}/libslirp-v${version}.tar.gz#f43e68b60b580647574ec4a0e2b6c600a56281e6c39f79426510832dc810f483"
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
