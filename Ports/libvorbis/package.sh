#!/usr/bin/env -S bash ../.port_include.sh
port=libvorbis
version=1.3.7
useconfigure=true
files="https://github.com/xiph/vorbis/releases/download/v${version}/libvorbis-${version}.tar.gz libvorbis-${version}.tar.gz"
depends=libogg
