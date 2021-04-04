#!/usr/bin/env -S bash ../.port_include.sh
port=curl
version=7.65.3
useconfigure=true
configopts="--disable-threaded-resolver --disable-ipv6"
files="https://curl.se/download/curl-${version}.tar.bz2 curl-${version}.tar.bz2
https://curl.se/download/curl-${version}.tar.bz2.asc curl-${version}.tar.bz2.asc"

depends=zlib
auth_type="sig"
auth_import_key="27EDEAF22F3ABCEB50DB9A125CC908FDB71E12C2"
auth_opts="curl-${version}.tar.bz2.asc curl-${version}.tar.bz2"

pre_configure() {
    # serenity's getaddrinfo exists but is a stub
    export curl_disallow_getaddrinfo=yes
}
