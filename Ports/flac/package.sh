#!/usr/bin/env -S bash ../.port_include.sh
port=flac
version=1.3.3
useconfigure=true
files="https://ftp.osuosl.org/pub/xiph/releases/flac/flac-${version}.tar.xz flac-${version}.tar.xz"
depends=libogg
