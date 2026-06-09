#!/usr/bin/env -S bash ../.port_include.sh
port='ffmpeg'
version='8.1.1'
useconfigure='true'
depends=(
    'bzip2'
    'libiconv'
    'libtiff'
    'SDL2'
    'x264'
    'x265'
    'xz'
)
files=(
    "https://ffmpeg.org/releases/ffmpeg-${version}.tar.gz#1b856f26a07082b6879f3e5300d81e8c7ce3b410ade5898b14382d90c2904634"
)
installopts=(
    "INSTALL_TOP=${SERENITY_INSTALL_ROOT}/usr/local"
)

configure() {
    run ./configure \
        --target-os=none \
        --arch="${SERENITY_ARCH}" \
        --cc="${CC} -std=gnu99" \
        --cxx="${CXX} -std=gnu99" \
        --enable-cross-compile \
        --enable-gpl \
        --enable-libx264 \
        --enable-libx265 \
        --enable-shared \
        --disable-stripping \
        --disable-avx
}

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
}
