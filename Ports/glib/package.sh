#!/usr/bin/env -S bash ../.port_include.sh
port='glib'
version='2.72.2'
files="https://gitlab.gnome.org/GNOME/glib/-/archive/${version}/glib-${version}.tar.gz glib-${version}.tar.gz 256439eb4667cda778f12faa6c1726413153618336514d3633ac1e4404d627ea"
auth_type='sha256'
useconfigure='true'
configopts=("--cross-file" "../cross_file-${SERENITY_ARCH}.txt")
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
