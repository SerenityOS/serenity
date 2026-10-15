#!/usr/bin/env -S bash ../.port_include.sh
port='openttd'
version='15.3'
depends=(
    'freetype'
    'libicu'
    'libpng'
    'openttd-opengfx'
    'openttd-opensfx'
    'SDL2'
    'xz'
    'zlib'
)
files=(
    "https://cdn.openttd.org/openttd-releases/${version}/openttd-${version}-source.tar.xz#5ea21eea7d59c78a42071924ac18c6bc0116088f2e96b14cfee9369175973be7"
)
useconfigure='true'
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_BUILD_TYPE=Release'
    '-DCMAKE_DISABLE_FIND_PACKAGE_OpenGL=ON'
)
launcher_name='OpenTTD'
launcher_category='&Games'
launcher_command='/usr/local/games/openttd'
icon_file='media/openttd.32.png'

configure() {
    host_env
    mkdir -p "${workdir}/host-build"
    (
        cd "${workdir}/host-build"
        cmake .. -DOPTION_TOOLS_ONLY=1
    )

    target_env
    mkdir -p "${workdir}/build"
    (
        cd "${workdir}/build"
        cmake .. "${configopts[@]}" -DHOST_BINARY_DIR="$(pwd)/../host-build"
    )
}

build() {
    host_env
    (
        cd "${workdir}/host-build"
        make "${makeopts[@]}"
    )

    target_env
    (
        cd "${workdir}/build"
        make "${makeopts[@]}"
    )
}

install() {
    (
        cd "${workdir}/build"
        make install
    )

    ln -sf /usr/local/games/openttd "${SERENITY_INSTALL_ROOT}/usr/local/bin/openttd"
}
