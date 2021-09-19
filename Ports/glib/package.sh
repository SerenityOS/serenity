#!/usr/bin/env -S bash ../.port_include.sh
port=glib
version=2.69.0
depends="libiconv libffi zlib gettext"
useconfigure=true
configopts="--cross-file ../cross_file-$SERENITY_ARCH.txt"
files="https://gitlab.gnome.org/GNOME/glib/-/archive/2.69.0/glib-${version}.tar.gz glib-${version}.tar.gz 35af83aedf34b96f1e99ed9c995c50960509f504750fb4def2440a9406b00a95"
auth_type=sha256

configure() {
    run meson _build $configopts
}

build() {
    run ninja -C _build
}

install() {
    export DESTDIR=$SERENITY_BUILD_DIR/Root
    run meson install -C _build
}
