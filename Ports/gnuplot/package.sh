#!/usr/bin/env -S bash ../.port_include.sh
port=gnuplot
version='5.4.8'
useconfigure=true
# Note: gnuplot's source code is hosted on SourceForge, but using the GitHub mirror makes downloading a versioned .tar.gz easier.
files=(
    "https://github.com/gnuplot/gnuplot/archive/${version}.tar.gz 2b0c1841640b2e33f8421ac83cd91d972d8b0c6acf9753f16385d5eec8a61a73"
)
configopts=("--prefix=${SERENITY_INSTALL_ROOT}/usr/local" "--with-readline=builtin" "--without-latex")

pre_configure() {
    run ./prepare
}

install() {
    run make install-strip
}
