#!/usr/bin/env -S bash ../.port_include.sh
port='glib'
version='2.73.3'
files="https://download.gnome.org/sources/glib/2.73/glib-${version}.tar.xz glib-${version}.tar.xz df1a2b841667d6b48b2ef6969ebda4328243829f6e45866726f806f90f64eead"
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
