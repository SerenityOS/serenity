#!/usr/bin/env -S bash ../.port_include.sh
port='glib'
version='2.77.2'
files=(
    "https://download.gnome.org/sources/glib/2.77/glib-${version}.tar.xz#16279739e4d30ec47be3e82909f5aeaaa41a8206bae3bead10a23fb2deff02a6"
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
