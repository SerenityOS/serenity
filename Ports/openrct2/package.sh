#!/usr/bin/env -S bash ../.port_include.sh
port='openrct2'
version='0.4.9'
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
    "https://github.com/OpenRCT2/OpenRCT2/archive/refs/tags/v${version}.tar.gz#d9eb7bb99923152122716707888a6182491662e106bef91b86106fa7b45b4309"
    "https://github.com/OpenRCT2/OpenRCT2/releases/download/v${version}/OpenRCT2-${version}-linux-jammy-x86_64.tar.gz#f388d46956f32755a504df1582fea7b800a8c100333667cb3f9c495c5c9d7201"
)
useconfigure='true'
workdir="OpenRCT2-${version}"
icon_file='resources/logo/icon.ico'
launcher_name='OpenRCT2'
launcher_category='&Games'
launcher_command='/usr/local/bin/openrct2'

configure() {
    mkdir -p "${PORT_BUILD_DIR}/OpenRCT2-${version}-build"
    cd "${PORT_BUILD_DIR}/OpenRCT2-${version}-build"
    cmake \
        -G Ninja \
        -DCMAKE_TOOLCHAIN_FILE="${SERENITY_BUILD_DIR}/CMakeToolchain.txt" \
        -DCMAKE_INSTALL_PREFIX="${DESTDIR}/usr/local/" \
        -DCMAKE_BUILD_TYPE='Release' \
        -DDISABLE_DISCORD_RPC='ON' \
        -DDISABLE_GOOGLE_BENCHMARK='ON' \
        -DWITH_TESTS='OFF' \
        -DDISABLE_OPENGL='ON' \
        "${PORT_BUILD_DIR}/OpenRCT2-${version}"
}

build() {
    cd "${PORT_BUILD_DIR}/OpenRCT2-${version}-build"
    [ -f 'g2.dat' ] || mv "${PORT_BUILD_DIR}/OpenRCT2/data/g2.dat" '.'
    ninja
}

install() {
    cd "${PORT_BUILD_DIR}/OpenRCT2-${version}-build"
    ninja install
}
