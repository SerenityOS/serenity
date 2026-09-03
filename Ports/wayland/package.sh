#!/usr/bin/env -S bash ../.port_include.sh
port='wayland'
version='1.26.0'
useconfigure='true'
configopts=(
    "--buildtype=release"
    "--cross-file=${SERENITY_BUILD_DIR}/meson-cross-file.txt"
    "-Dlibraries=true"
    "-Dscanner=true"
    "-Dtests=false"
    "-Ddocumentation=false"
)
depends=(
    'expat'
    'libffi'
    'libxml2'
)
files=(
    "https://gitlab.freedesktop.org/wayland/wayland/-/releases/${version}/downloads/wayland-${version}.tar.xz#64176eaa46e4969903e286f8e5ef8331affc17fdf03ac9b58381d2b23162b7a3"
)

configure() {
    # TODO: Figure out why GCC doesn't autodetect that libgcc_s is needed.
    if [ "${SERENITY_TOOLCHAIN}" = "GNU" ]; then
        export LDFLAGS="-lgcc_s"
    fi

    run meson build "${configopts[@]}"
}

build() {
    run ninja -C build
}

install() {
    export DESTDIR="${SERENITY_INSTALL_ROOT}"
    run meson install -C build
}
