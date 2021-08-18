#!/usr/bin/env -S bash ../.port_include.sh
port=slang
version=2.3.2
useconfigure=true
files="https://www.jedsoft.org/releases/slang/slang-${version}.tar.bz2 slang-${version}.tar.bz2 fc9e3b0fc4f67c3c1f6d43c90c16a5c42d117b8e28457c5b46831b8b5d3ae31a"
auth_type=sha256
depends="zlib libiconv pcre libpng"

build() {
    # The Makefile's dependencies are broken, so -jN won't work for this port.
    run make
}
