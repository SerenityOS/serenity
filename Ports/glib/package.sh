#!/usr/bin/env -S bash ../.port_include.sh
port='glib'
version='2.85.2'
files=(
    "https://download.gnome.org/sources/glib/2.85/glib-${version}.tar.xz#833b97c0f0a1bfdba1d0fbfc36cd368b855c5afd9f02b8ffb24129114ad051b2"
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
