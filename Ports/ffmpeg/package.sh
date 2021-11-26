#!/usr/bin/env -S bash ../.port_include.sh
port=ffmpeg
version=4.4
useconfigure=true
depends=("libiconv" "libtiff" "xz" "bzip2")
files="https://ffmpeg.org/releases/ffmpeg-${version}.tar.gz ffmpeg-${version}.tar.gz a4abede145de22eaf233baa1726e38e137f5698d9edd61b5763cd02b883f3c7c"
auth_type="sha256"
installopts=("INSTALL_TOP=${SERENITY_INSTALL_ROOT}/usr/local")
configopts=("SRC_PATH=.")

configure() {
    run ./configure \
        --arch="x86_32" \
        --cc="${CC} -std=gnu99" \
        --cxx="${CXX} -std=gnu99" \
        --disable-network \
        --enable-cross-compile
}

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    for lib in libavcodec libavdevice libavfilter libavformat libavutil; do
        ${CC} -shared -o ${SERENITY_INSTALL_ROOT}/usr/local/lib/${lib}.so -Wl,-soname,${lib}.so -Wl,--whole-archive ${SERENITY_INSTALL_ROOT}/usr/local/lib/${lib}.a -Wl,--no-whole-archive -liconv -ltiff -llzma -lbz2
        rm -f ${SERENITY_INSTALL_ROOT}/usr/local/lib/$lib.la
    done
}
