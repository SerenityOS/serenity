#!/usr/bin/env -S bash ../.port_include.sh
port='pango'
# pango 1.55 requires fontconfig 2.15 but SerenityOS uses fontconfig 2.14
version='1.54.0'
archive_hash='8a9eed75021ee734d7fc0fdf3a65c3bba51dfefe4ae51a9b414a60c70b2d1ed8'
files=(
    "https://download.gnome.org/sources/pango/${version%.*}/pango-${version}.tar.xz#${archive_hash}"
)
useconfigure='true'
configopts=(
    "--cross-file=${SERENITY_BUILD_DIR}/meson-cross-file.txt"
    "--prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    '-Dbuildtype=release'
    '-Dbuild-testsuite=false'
    '-Dbuild-examples=false'
    # Use patched ports instead of subprojects.
    '--wrap-mode=nofallback'
    '-Dcairo=enabled'
    '-Dfontconfig=enabled'
    '-Dfreetype=enabled'
)
depends=(
    'cairo'
    'expat'
    'fontconfig'
    'freetype'
    'fribidi'
    'glib'
    'harfbuzz'
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
