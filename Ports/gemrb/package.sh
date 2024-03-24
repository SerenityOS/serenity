#!/usr/bin/env -S bash ../.port_include.sh
port='gemrb'
version='0.9.2'
useconfigure='true'
files=(
    "https://github.com/gemrb/gemrb/archive/refs/tags/v${version}.tar.gz#ea614c067483606dab680ab18cd50527f56803bd46e0888e3c786eec05d3bb7d"
)
depends=(
    'freetype'
    'libiconv'
    'python3'
    'SDL2'
    'SDL2_mixer'
    'zlib'
)
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DCMAKE_INSTALL_PREFIX=${DESTDIR}/usr/local/"
    '-DCMAKE_BUILD_TYPE=Release'
    '-DDISABLE_WERROR=1'
    '-DSTATIC_LINK=ON'
    '-DSDL_BACKEND=SDL2'
)
icon_file='artwork/gemrb-logo.ico'
launcher_name='GemRB'
launcher_category='&Games'
launcher_command='/usr/local/bin/gemrb'

configure() {
    mkdir -p "${PORT_BUILD_DIR}/gemrb-${version}-build"
    cd "${PORT_BUILD_DIR}/gemrb-${version}-build"
    cmake -G Ninja "${configopts[@]}" "${PORT_BUILD_DIR}/gemrb-${version}"
}

build() {
    ninja -C "${PORT_BUILD_DIR}/gemrb-${version}-build"
}

install() {
    ninja -C "${PORT_BUILD_DIR}/gemrb-${version}-build" install
}
