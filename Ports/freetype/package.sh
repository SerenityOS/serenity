#!/usr/bin/env -S bash ../.port_include.sh
port=freetype
version=2.10.4
useconfigure=true
files="https://download.savannah.gnu.org/releases/freetype/freetype-${version}.tar.gz freetype-${version}.tar.gz 5eab795ebb23ac77001cfb68b7d4d50b5d6c7469247b0b01b2c953269f658dac"
auth_type=sha256
configopts="--with-brotli=no --with-bzip2=no --with-zlib=no --with-harfbuzz=no --with-png=no"
