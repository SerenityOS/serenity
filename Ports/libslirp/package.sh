#!/usr/bin/env -S bash ../.port_include.sh
port='libslirp'
version='4.8.0'
workdir="libslirp-v${version}"
files=(
    "https://gitlab.freedesktop.org/slirp/libslirp/-/archive/v${version}/libslirp-v${version}.tar.gz#2a98852e65666db313481943e7a1997abff0183bd9bea80caec1b5da89fda28c"
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
