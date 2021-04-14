#!/usr/bin/env -S bash ../.port_include.sh
port=ffmpeg
version=4.3.2
useconfigure=true
files="https://ffmpeg.org/releases/ffmpeg-${version}.tar.xz ffmpeg-${version}.tar.xz
https://ffmpeg.org/releases/ffmpeg-${version}.tar.xz.asc ffmpeg-${version}.tar.xz.asc"
makeopts="HOSTCC=${CC} HOSTLD=${CC}"
depends="SDL2"

configure() {
	run ./configure --enable-cross-compile --target-os=serenity --arch=i686 \
	    --enable-ffmpeg --enable-ffprobe --enable-ffplay --cross-prefix=${CC%gcc} \
	    --disable-txtpages --disable-manpages --disable-podpages \
	    --disable-static --enable-shared \
	    --enable-debug --disable-stripping # Remove Before PR!
}
