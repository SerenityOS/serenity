#!/bin/bash ../.port_include.sh
port=gnuplot
version=5.2.8
useconfigure=true
# Note: gnuplot's source code is hosted on SourceForge, but using the GitHub mirror makes downloading a versioned .tar.gz easier.
files="https://github.com/gnuplot/gnuplot/archive/${version}.tar.gz gnuplot-${version}.tar.gz"
configopts="--prefix=$SERENITY_ROOT/Build/Root/usr/local --with-readline=builtin"

pre_configure() {
    run ./prepare
}

install() {
    run make install-strip
}
