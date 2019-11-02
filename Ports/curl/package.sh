#!/bin/sh ../.port_include.sh
port=curl
version=7.65.3
useconfigure=true
configopts="--disable-threaded-resolver"
files="https://curl.haxx.se/download/curl-7.65.3.tar.bz2 curl-7.65.3.tar.bz2"
depends=zlib
