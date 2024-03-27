#!/usr/bin/env -S bash ../.port_include.sh
port='glib'
version='2.80.0'
files=(
    "https://download.gnome.org/sources/glib/2.80/glib-${version}.tar.xz#8228a92f92a412160b139ae68b6345bd28f24434a7b5af150ebe21ff587a561d"
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
