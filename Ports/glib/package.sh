#!/usr/bin/env -S bash ../.port_include.sh
port=glib
version=2.72.1
depends=("libiconv" "libffi" "zlib" "gettext" "pcre")
useconfigure=true
configopts=("--cross-file" "../cross_file-$SERENITY_ARCH.txt")
files="https://gitlab.gnome.org/GNOME/glib/-/archive/${version}/glib-${version}.tar.gz glib-${version}.tar.gz 4a345987a9ee7709417f5a5c6f4eeec2497bc2a913f14c1b9bdc403409d5ffb7"
auth_type=sha256

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
    export DESTDIR=$SERENITY_BUILD_DIR/Root
    run meson install -C _build
}
