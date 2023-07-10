#!/usr/bin/env -S bash ../.port_include.sh
port=ffmpeg
version=5.0
useconfigure=true
depends=("libiconv" "libtiff" "xz" "bzip2" "SDL2" "x264" "x265")
files=(
    "https://ffmpeg.org/releases/ffmpeg-${version}.tar.gz ffmpeg-${version}.tar.gz 7bf52bc242b5db8df67c62cb826df134d917dedcf6abf1289e15e4057bcc1750"
)
installopts=("INSTALL_TOP=${SERENITY_INSTALL_ROOT}/usr/local")
configopts=("SRC_PATH=.")

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
