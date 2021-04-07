#!/usr/bin/env -S bash ../.port_include.sh
port=ffmpeg
version=4.3.2
useconfigure=true
files="https://ffmpeg.org/releases/ffmpeg-${version}.tar.xz ffmpeg-${version}.tar.xz
https://ffmpeg.org/releases/ffmpeg-${version}.tar.xz.asc ffmpeg-${version}.tar.xz.asc"
makeopts="HOSTCC=${CC} HOSTLD=${CC}"
depends=

configure() {
	run ./configure --disable-everything --enable-cross-compile --target-os=serenity --arch=i386 --disable-txtpages --disable-manpages --disable-podpages --disable-htmlpages --cross-prefix=${CC%gcc}
}
