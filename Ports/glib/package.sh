#!/usr/bin/env -S bash ../.port_include.sh
port='glib'
version='2.77.0'
files=(
    "https://download.gnome.org/sources/glib/2.77/glib-${version}.tar.xz#1897fd8ad4ebb523c32fabe7508c3b0b039c089661ae1e7917df0956a320ac4d"
)
useconfigure='true'
configopts=(
    '--cross-file' "${SERENITY_BUILD_DIR}/meson-cross-file.txt"
)
depends=(
    'gettext'
    'libffi'
    'libiconv'
    'pcre2'
    'zlib'
)

configure() {
    # TODO: Figure out why GCC doesn't autodetect that libgcc_s is needed.
    if [ "${SERENITY_TOOLCHAIN}" = "GNU" ]; then
        export LDFLAGS="-lgcc_s"
    fi

    run meson _build "${configopts[@]}"
}

build() {
    run ninja -C _build
}

install() {
    export DESTDIR="${SERENITY_INSTALL_ROOT}"
    run meson install -C _build
}
