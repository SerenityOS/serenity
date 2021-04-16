#!/usr/bin/env -S bash ../.port_include.sh
port=freetype
version=2.10.4
useconfigure=true
files="https://download.savannah.gnu.org/releases/freetype/freetype-${version}.tar.gz freetype-${version}.tar.gz 4934a8b61b636920bcce58e7c7f3e1a2"
auth_type=md5
configopts="--with-brotli=no --with-bzip2=no --with-zlib=no --with-harfbuzz=no --with-png=no"
