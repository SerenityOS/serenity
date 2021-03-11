#!/usr/bin/env -S bash ../.port_include.sh
port=bzip2
version=1.0.8
files="https://sourceware.org/pub/bzip2/bzip2-${version}.tar.gz bzip2-${version}.tar.gz"
workdir="bzip2-$version"
makeopts="bzip2 CC=${CC}"
installopts="PREFIX=${SERENITY_BUILD_DIR}/Root/usr/local"
