#!/usr/bin/env -S bash ../.port_include.sh
port=gnuplot
version=5.2.8
useconfigure=true
# Note: gnuplot's source code is hosted on SourceForge, but using the GitHub mirror makes downloading a versioned .tar.gz easier.
files="https://github.com/gnuplot/gnuplot/archive/${version}.tar.gz gnuplot-${version}.tar.gz b55f591e2ad9d01ffca821f2f91ce781b481bb5dd602ce5188bfad3140f44ac0"
auth_type=sha256
configopts=("--prefix=${SERENITY_INSTALL_ROOT}/usr/local" "--with-readline=builtin" "--without-latex")

pre_configure() {
    run ./prepare
}

install() {
    run make install-strip
}
