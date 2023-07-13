#!/usr/bin/env -S bash ../.port_include.sh
port='glib'
version='2.76.1'
files="https://download.gnome.org/sources/glib/2.76/glib-${version}.tar.xz glib-${version}.tar.xz 43dc0f6a126958f5b454136c4398eab420249c16171a769784486e25f2fda19f"
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
