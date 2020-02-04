#!/bin/bash ../.port_include.sh
port=curl
version=7.65.3
useconfigure=true
configopts="--disable-threaded-resolver"
files="https://curl.haxx.se/download/curl-${version}.tar.bz2 curl-${version}.tar.bz2
https://curl.haxx.se/download/curl-${version}.tar.bz2.asc curl-${version}.tar.bz2.asc"

depends=zlib
auth_type="sig"
auth_import_key="27EDEAF22F3ABCEB50DB9A125CC908FDB71E12C2"
auth_opts="curl-${version}.tar.bz2.asc curl-${version}.tar.bz2"
