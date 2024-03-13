#!/usr/bin/env -S bash ../.port_include.sh
port='lite-xl'
version='2.1.3'
useconfigure='true'
configopts=(
    "--buildtype=release"
    "--cross-file=${SERENITY_BUILD_DIR}/meson-cross-file.txt"
)
depends=(
    'freetype'
    'lua'
    'pcre2'
    'SDL2'
)
files=(
    "https://github.com/lite-xl/lite-xl/archive/refs/tags/v${version}.tar.gz#a13c423588a5549e42fda7dfe9064bd3776b6202c170c7bb493d96a692360383"
)
workdir="lite-xl-${version}"
launcher_name='Lite-XL'
icon_file='resources/icons/icon.ico'
launcher_category='D&evelopment'
launcher_command='/usr/local/bin/lite-xl'
launcher_run_in_terminal='false'

configure() {
    # TODO: Figure out why GCC doesn't autodetect that libgcc_s is needed.
    if [ "${SERENITY_TOOLCHAIN}" = "GNU" ]; then
        export LDFLAGS="-lgcc_s"
    fi

    run meson build "${configopts[@]}"
}

build() {
    run ninja -C build
}

install() {
    export DESTDIR="${SERENITY_INSTALL_ROOT}"
    run meson install -C build
}
