#!/usr/bin/env -S bash ../.port_include.sh
port='ffmpeg'
version='6.0'
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
    "https://ffmpeg.org/releases/ffmpeg-${version}.tar.gz#f4ccf961403752c93961927715f524576d1f4dd02cd76d8c76aed3bbe6686656"
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
