#!/usr/bin/env -S bash ../.port_include.sh
port=glib
version=2.70.0
depends=("libiconv" "libffi" "zlib" "gettext" "pcre")
useconfigure=true
configopts=("--cross-file" "../cross_file-$SERENITY_ARCH.txt")
files="https://gitlab.gnome.org/GNOME/glib/-/archive/${version}/glib-${version}.tar.gz glib-${version}.tar.gz aadf815ed908d4cc14ac3976f325b986b4ab2b65ad85bc214ddf2e200648bd1c"
auth_type=sha256

configure() {
    run meson _build "${configopts[@]}"
}

build() {
    run ninja -C _build
}

install() {
    export DESTDIR=$SERENITY_BUILD_DIR/Root
    run meson install -C _build
}
