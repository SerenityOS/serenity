#!/usr/bin/env -S bash ../.port_include.sh
port='libfuse'
version='3.18.3'
files=(
    "https://github.com/libfuse/libfuse/releases/download/fuse-${version}/fuse-${version}.tar.gz#bcd19582c5e30f7fe45dd86a5540e998590aa01903afc7ebcbeea6c8ac5421ee"
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
