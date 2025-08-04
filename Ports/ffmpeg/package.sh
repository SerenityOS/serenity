#!/usr/bin/env -S bash ../.port_include.sh
port='ffmpeg'
version='7.1.1'
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
    "https://ffmpeg.org/releases/ffmpeg-${version}.tar.gz#9a6e57a446b671012612aaeb9df5126794d5ac8f2015ca220934f99a6a4e0601"
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
