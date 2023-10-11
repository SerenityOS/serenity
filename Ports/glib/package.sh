#!/usr/bin/env -S bash ../.port_include.sh
port='glib'
version='2.78.0'
files=(
    "https://download.gnome.org/sources/glib/2.78/glib-${version}.tar.xz#44eaab8b720877ce303c5540b657b126f12dc94972d9880b52959f43fb537b30"
)
useconfigure='true'
configopts=(
    '--cross-file'
    "${SERENITY_BUILD_DIR}/meson-cross-file.txt"
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
    if [ "${SERENITY_TOOLCHAIN}" = 'GNU' ]; then
        export LDFLAGS='-lgcc_s'
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
