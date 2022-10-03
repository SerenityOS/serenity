#!/usr/bin/env -S bash ../.port_include.sh
port='glib'
version='2.74.0'
files="https://download.gnome.org/sources/glib/2.74/glib-${version}.tar.xz glib-${version}.tar.xz 3652c7f072d7b031a6b5edd623f77ebc5dcd2ae698598abcc89ff39ca75add30"
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
