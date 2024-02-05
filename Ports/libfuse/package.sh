#!/usr/bin/env -S bash ../.port_include.sh
port='libfuse'
version='3.16.2'
files=(
    "https://github.com/libfuse/libfuse/releases/download/fuse-${version}/fuse-${version}.tar.gz#f797055d9296b275e981f5f62d4e32e089614fc253d1ef2985851025b8a0ce87"
)
useconfigure='true'
configopts=(
    "--cross-file=${SERENITY_BUILD_DIR}/meson-cross-file.txt"
)
workdir="fuse-${version}"

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
