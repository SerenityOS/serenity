#!/usr/bin/env -S bash ../.port_include.sh
port='openrct2'
version='0.4.3'
auth_type='sha256'
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
files="https://github.com/OpenRCT2/OpenRCT2/archive/refs/tags/v${version}.tar.gz ${port}-${version}.tar.gz 1269759b3a90717f379cd375f30629e3f24d3fdeb0c54cc1dca03676e64731ef
https://github.com/OpenRCT2/OpenRCT2/releases/download/v${version}/OpenRCT2-${version}-linux-x86_64.tar.gz OpenRCT2-${version}-linux-x86_64.tar.gz 3691aa42e0b2eff80609688930d87b8cb8b97fb57f2ed624d3ed57e2eefb4fcf"
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
