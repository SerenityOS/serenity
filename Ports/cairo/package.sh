#!/usr/bin/env -S bash ../.port_include.sh
port='cairo'
version='1.18.6'
files=(
    "https://cairographics.org/releases/cairo-${version}.tar.xz#1c767308174337a74694da0f3ec069c271452163a1ef4540964c50c301f157d4"
)
useconfigure='true'
configopts=(
    "--cross-file=${SERENITY_BUILD_DIR}/meson-cross-file.txt"
    "--prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    '-Dbuildtype=release'
    '-Dtests=disabled'
    # Use patched ports instead of subprojects.
    '--wrap-mode=nofallback'
    '-Dfontconfig=enabled'
    '-Dfreetype=enabled'
    '-Dglib=enabled'
    '-Dpng=enabled'
    '-Dzlib=enabled'
)
depends=(
    'expat'
    'fontconfig'
    'freetype'
    'glib'
    'libpng'
    'pixman'
    'zlib'
)

configure() {
    run meson setup --reconfigure "${configopts[@]}" build
}

build() {
    run ninja -C build
}

install() {
    run ninja -C build install
}
