#!/usr/bin/env -S bash ../.port_include.sh
port=ffmpeg
version=5.0
useconfigure=true
depends=("libiconv" "libtiff" "xz" "bzip2" "SDL2")
files="https://ffmpeg.org/releases/ffmpeg-${version}.tar.gz ffmpeg-${version}.tar.gz 7bf52bc242b5db8df67c62cb826df134d917dedcf6abf1289e15e4057bcc1750"
auth_type="sha256"
installopts=("INSTALL_TOP=${SERENITY_INSTALL_ROOT}/usr/local")
configopts=("SRC_PATH=.")

configure() {
    run ./configure \
        --target-os=none \
        --arch="${SERENITY_ARCH}" \
        --cc="${CC} -std=gnu99" \
        --cxx="${CXX} -std=gnu99" \
        --enable-cross-compile \
	--disable-stripping \
	--disable-avx
}

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    for lib in libavcodec libavdevice libavfilter libavformat libavutil; do
        ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/${lib}.so -Wl,-soname,${lib}.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/${lib}.a -Wl,--no-whole-archive -liconv -ltiff -llzma -lbz2
        rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/$lib.la
    done
}
