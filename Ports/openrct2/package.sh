#!/usr/bin/env -S bash ../.port_include.sh
port='openrct2'
version='0.4.5'
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
    "https://github.com/OpenRCT2/OpenRCT2/archive/refs/tags/v${version}.tar.gz#eb9e20dc0dcbf10b02b5f334a029983a0a2b43d931f95a2088a64e8b2657bab1"
    "https://github.com/OpenRCT2/OpenRCT2/releases/download/v${version}/OpenRCT2-${version}-linux-jammy-x86_64.tar.gz#c0652ace6fd2302e77cd25b85bb973e3c0d2c8be1d20cfdcb7a2a9b15630d112"
)
useconfigure='true'
workdir="OpenRCT2-${version}"
icon_file='resources/logo/icon_x16.png'
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
