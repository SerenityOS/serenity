#!/usr/bin/env -S bash ../.port_include.sh
port='ffmpeg'
version='9.0.2'
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
    "https://ffmpeg.org/releases/ffmpeg-${version}.tar.gz#84960df915059e8754fef2cd7c9afeb614062b1b5458ec471eecee619ee04e98"
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
