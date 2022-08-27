#!/usr/bin/env -S bash ../.port_include.sh
port='glib'
version='2.73.0'
files="https://download.gnome.org/sources/glib/2.73/glib-${version}.tar.xz glib-${version}.tar.xz 3673f10515f4bcfb9ee2ce0a921a18fa359c36fab388b19819467e7b09506870"
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
