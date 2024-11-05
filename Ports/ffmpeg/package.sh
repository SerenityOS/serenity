#!/usr/bin/env -S bash ../.port_include.sh
port='ffmpeg'
version='7.0.2'
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
    "https://ffmpeg.org/releases/ffmpeg-${version}.tar.gz#1233b3a93dd7517cc3c56b72a67f64041c044848d981e3deff4bebffa25f1054"
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
