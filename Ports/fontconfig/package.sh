#!/usr/bin/env -S bash ../.port_include.sh
port='fontconfig'
version='2.18.3'
depends=(
    'freetype'
    'libxml2'
)
files=(
    "https://gitlab.freedesktop.org/fontconfig/fontconfig/-/archive/${version}/fontconfig-${version}.tar.gz#9ae01e1d53acdef56010c5451cd34aa41d325b2faccd8606448d8fa01b2496b3"
)
useconfigure='true'
configopts=(
    "--cross-file=${SERENITY_BUILD_DIR}/meson-cross-file.txt"
    '--buildtype=release'
    '-Ddoc=disabled'
    '-Dtests=disabled'
    '-Dnls=disabled'
    '-Dxml-backend=libxml2'
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
