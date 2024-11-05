#!/usr/bin/env -S bash ../.port_include.sh
port='gnuplot'
version='5.4.9'
useconfigure='true'
# Note: gnuplot's source code is hosted on SourceForge, but using the GitHub mirror makes downloading a versioned .tar.gz easier.
files=(
    "https://github.com/gnuplot/gnuplot/archive/${version}.tar.gz#0806ab023193e0b303db443167783ae9f51622a044a8a08af781088bbb4aea53"
)
depends=(
    'libgd'
    'lua'
)
configopts=(
    "--prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    '--with-qt=no'
    '--with-readline=builtin'
    '--without-cairo'
    '--without-latex'
    'libgd_LIBS=-liconv -lfreetype -lfontconfig -lpng'
)

pre_configure() {
    run ./prepare
}

install() {
    run make install-strip
}
