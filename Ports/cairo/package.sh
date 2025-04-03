#!/usr/bin/env -S bash ../.port_include.sh
port='cairo'
version='1.18.2'
archive_hash='a62b9bb42425e844cc3d6ddde043ff39dbabedd1542eba57a2eb79f85889d45a'
files=(
    "https://cairographics.org/releases/cairo-${version}.tar.xz#${archive_hash}"
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
