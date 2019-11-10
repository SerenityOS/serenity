#!/bin/bash ../.port_include.sh
port=libarchive
version=3.4.0
useconfigure=true
configopts="--without-xml2"
files="https://libarchive.org/downloads/libarchive-3.4.0.tar.gz libarchive-3.4.0.tar.gz"
depends=zlib
