#!/usr/bin/env -S bash ../.port_include.sh
port='openrct2'
version='0.5.7'
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
    "https://github.com/OpenRCT2/OpenRCT2/archive/refs/tags/v${version}.tar.gz#00b76009e4bb64e7a431ae2c7f0ebcad6e6567a30e79ca07bf9221ac14b19b8b"
    "https://github.com/OpenRCT2/OpenRCT2/releases/download/v${version}/OpenRCT2-v${version}-Linux-noble-x86_64.tar.gz#7f94ff9857e9a45b61f518405e3ed7299f84385d123468464d71cb94c6b6cd86"
)
useconfigure='true'
workdir="OpenRCT2-${version}"
icon_file='resources/logo/icon.ico'
launcher_name='OpenRCT2'
launcher_category='&Games'
launcher_command='/usr/local/bin/openrct2'
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
    '-DCMAKE_BUILD_TYPE=Release'
    '-DDISABLE_DISCORD_RPC=ON'
    '-DDISABLE_GOOGLE_BENCHMARK=ON'
    '-DWITH_TESTS=OFF'
    '-DDISABLE_OPENGL=ON'
    '-DDOWNLOAD_TITLE_SEQUENCES=OFF'
    '-DDOWNLOAD_OBJECTS=OFF'
    '-DDOWNLOAD_OPENSFX=OFF'
    '-DDOWNLOAD_OPENMUSIC=OFF'
)

configure() {
    run cmake -G Ninja -B build -S . "${configopts[@]}"
}

build() {
    for data_file in g2 fonts palettes tracks; do
        [ -f "${workdir}/build/${data_file}.dat" ] || cp "${PORT_BUILD_DIR}/OpenRCT2/data/${data_file}.dat" "${workdir}/build/"
    done
    run cmake --build build
}

install() {
    run cmake --install build
    for data_dir in assetpack sequence object; do
        mkdir -p "${DESTDIR}/usr/local/share/openrct2/${data_dir}"
        cp -a "${PORT_BUILD_DIR}/OpenRCT2/data/${data_dir}/." "${DESTDIR}/usr/local/share/openrct2/${data_dir}/"
    done
}
