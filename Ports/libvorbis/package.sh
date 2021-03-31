#!/usr/bin/env -S bash ../.port_include.sh
port=libvorbis
version=1.3.7
useconfigure=true
configopts="--prefix=${SERENITY_BUILD_DIR}/Root/usr/local"
files="https://github.com/xiph/vorbis/releases/download/v${version}/libvorbis-${version}.tar.gz libvorbis-${version}.tar.gz"
depends=libogg
