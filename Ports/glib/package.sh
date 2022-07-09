#!/usr/bin/env -S bash ../.port_include.sh
port='glib'
version='2.73.0'
files="https://gitlab.gnome.org/GNOME/glib/-/archive/${version}/glib-${version}.tar.gz glib-${version}.tar.gz 3f573319adbdf572d79255e8bae85c7e2902d1aa6177d2646605a00c0a607eca"
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
