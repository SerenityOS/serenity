#!/usr/bin/env -S bash ../.port_include.sh
port='openrct2'
version='0.4.4'
depends=(
    'curl'
    'flac'
    'fontconfig'
    'freetype'
    'libicu'
    'libogg'
    'libpng'
    'libvorbis'
    'libzip'
    'nlohmann-json'
    'openssl'
    'SDL2'
    'speexdsp'
    'zlib'
)
files=(
    "https://github.com/OpenRCT2/OpenRCT2/archive/refs/tags/v${version}.tar.gz#18970bfffe49c77fa81ea6c295119b173a613c7310d7762963458e3e77c24913"
    "https://github.com/OpenRCT2/OpenRCT2/releases/download/v${version}/OpenRCT2-${version}-linux-jammy-x86_64.tar.gz#e4263121ec51a1340d269e07b366f7ec306013e292babfea9fd768c03e19b5b7"
)
useconfigure='true'
workdir="OpenRCT2-${version}"
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    "-DCMAKE_INSTALL_PREFIX=${DESTDIR}/usr/local/"
    "-DCMAKE_BUILD_TYPE=Release"
    "-DDISABLE_DISCORD_RPC=ON"
    "-DDISABLE_GOOGLE_BENCHMARK=ON"
    "-DWITH_TESTS=OFF"
    "-DDISABLE_OPENGL=ON")
icon_file='resources/logo/icon_x16.png'
launcher_name='OpenRCT2'
launcher_category='Games'
launcher_command='/usr/local/bin/openrct2'

configure() {
    mkdir -p "${PORT_BUILD_DIR}/OpenRCT2-${version}-build"
    cd "${PORT_BUILD_DIR}/OpenRCT2-${version}-build"
    cmake -G Ninja "${configopts[@]}" "${PORT_BUILD_DIR}/OpenRCT2-${version}"
}

build() {
    cd "${PORT_BUILD_DIR}/OpenRCT2-${version}-build"
    mv "${PORT_BUILD_DIR}/OpenRCT2/data/g2.dat" .
    ninja
}

install() {
    cd "${PORT_BUILD_DIR}/OpenRCT2-${version}-build"
    ninja install
}
