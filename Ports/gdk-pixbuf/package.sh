#!/usr/bin/env -S bash ../.port_include.sh
port='gdk-pixbuf'
version='2.42.12'
archive_hash='b9505b3445b9a7e48ced34760c3bcb73e966df3ac94c95a148cb669ab748e3c7'
files=(
    "https://download.gnome.org/sources/gdk-pixbuf/${version%.*}/gdk-pixbuf-${version}.tar.xz#${archive_hash}"
)
useconfigure='true'
configopts=(
    "--cross-file=${SERENITY_BUILD_DIR}/meson-cross-file.txt"
    "--prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    '-Dbuildtype=release'
    '-Dtests=false'
    '-Dinstalled_tests=false'
    '-Dman=false'
    # gio_sniffing requres shared-mime-info
    '-Dgio_sniffing=false'
    # Use patched ports instead of subprojects.
    '--wrap-mode=nofallback'
    '-Dtiff=enabled'
)
depends=(
    'glib'
    'libpng'
    'libjpeg'
    'libtiff'
)

configure() {
    run meson setup build "${configopts[@]}"
}

build() {
    run ninja -C build
}

install() {
    run ninja -C build install
}
