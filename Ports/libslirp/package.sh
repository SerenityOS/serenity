#!/usr/bin/env -S bash ../.port_include.sh
port='libslirp'
version='4.9.4'
workdir="libslirp-v${version}"
files=(
    "https://gitlab.freedesktop.org/slirp/libslirp/-/archive/v${version}/libslirp-v${version}.tar.gz#3998863b020aeda34bddc567097c6efba55a78cdf6eeee6bcd42c11ef23967da"
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
