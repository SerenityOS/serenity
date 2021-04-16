#!/usr/bin/env -S bash ../.port_include.sh
port=gnuplot
version=5.2.8
useconfigure=true
# Note: gnuplot's source code is hosted on SourceForge, but using the GitHub mirror makes downloading a versioned .tar.gz easier.
files="https://github.com/gnuplot/gnuplot/archive/${version}.tar.gz gnuplot-${version}.tar.gz 292f983e273cd50cf02e0737043fae0e"
auth_type=md5
configopts="--prefix=${SERENITY_BUILD_DIR}/Root/usr/local --with-readline=builtin --without-latex"

pre_configure() {
    run ./prepare
}

install() {
    run make install-strip
}
