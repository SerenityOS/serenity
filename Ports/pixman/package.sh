#!/usr/bin/env -S bash ../.port_include.sh
port='pixman'
version='0.46.4'
useconfigure='true'
files=(
    "https://www.cairographics.org/releases/pixman-${version}.tar.gz#d09c44ebc3bd5bee7021c79f922fe8fb2fb57f7320f55e97ff9914d2346a591c"
)
configopts=(
    "--cross-file=${SERENITY_BUILD_DIR}/meson-cross-file.txt"
    '--buildtype=release'
    '-Dtests=disabled'
    '-Ddemos=disabled'
    '-Dgtk=disabled'
    # libpng is only used by the (disabled) test/demo programs
    '-Dlibpng=disabled'
    '-Dopenmp=disabled'
)

configure() {
    run meson setup build "${configopts[@]}"
}

build() {
    run ninja -C build
}

install() {
    export DESTDIR="${SERENITY_INSTALL_ROOT}"
    run meson install -C build
}
