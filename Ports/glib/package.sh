#!/usr/bin/env -S bash ../.port_include.sh
port='glib'
version='2.75.2'
files="https://download.gnome.org/sources/glib/2.75/glib-${version}.tar.xz glib-${version}.tar.xz 360d6fb75202c0eb0d07f0ab812b19b526f1c05ccc0a8ed7e5d2c988616d343a"
auth_type='sha256'
useconfigure='true'
configopts=("--cross-file" "${SERENITY_BUILD_DIR}/meson-cross-file.txt")
depends=("libiconv" "libffi" "zlib" "gettext" "pcre")

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
