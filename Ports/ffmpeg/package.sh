#!/usr/bin/env -S bash ../.port_include.sh
port=ffmpeg
version=4.4
useconfigure=true
files="https://ffmpeg.org/releases/ffmpeg-${version}.tar.gz ffmpeg-${version}.tar.gz a4abede145de22eaf233baa1726e38e137f5698d9edd61b5763cd02b883f3c7c"
auth_type="sha256"
installopts="INSTALL_TOP=${SERENITY_INSTALL_ROOT}/usr/local"
configopts="SRC_PATH=."

configure() {
    run ./configure \
        --arch="x86_32" \
        --cc="${CC} -std=gnu99" \
        --cxx="${CXX} -std=gnu99" \
        --disable-network \
        --enable-cross-compile
}
