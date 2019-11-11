#!/bin/bash ../.port_include.sh
port=python-3.6
version=3.6
workdir=Python-3.6.0
useconfigure=true
configopts="--build=i686 --without-threads --enable-optimizations"
makeopts="-j$(nproc) build_all"
installopts="-j$(nproc) build_all"
files="https://www.python.org/ftp/python/3.6.0/Python-3.6.0.tar.xz Python-3.6.0.tar.xz"

export CONFIG_SITE=$(pwd)/config.site
