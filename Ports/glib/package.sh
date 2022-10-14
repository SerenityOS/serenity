#!/usr/bin/env -S bash ../.port_include.sh
port='glib'
version='2.74.1'
files="https://download.gnome.org/sources/glib/2.74/glib-${version}.tar.xz glib-${version}.tar.xz 0ab981618d1db47845e56417b0d7c123f81a3427b2b9c93f5a46ff5bbb964964"
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
