#!/usr/bin/env -S bash ../.port_include.sh
port='gnuplot'
version='6.0.5'
useconfigure='true'
files=(
    "https://downloads.sourceforge.net/project/gnuplot/gnuplot/${version}/gnuplot-${version}.tar.gz#73237f37f03306d68bfae133a9a50d5e9341384e198d5ab37eeca9ab534deed8"
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

install() {
    run make install-strip
}
