#!/usr/bin/env -S bash ../.port_include.sh
port='lite-xl'
version='2.1.0'
commit_hash='97ba91af8b855a10a14bdc5cad774c877156f4a9'
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
    "https://github.com/lite-xl/lite-xl/archive/${commit_hash}.tar.gz#25a0dd0a6ef856fd312eecd54983d401224eb8d7a5d5aa4a37c9131ac77bd9ca"
)
workdir="lite-xl-${commit_hash}"
launcher_name='Lite-XL'
icon_file='resources/icons/icon.ico'
launcher_category='Development'
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
