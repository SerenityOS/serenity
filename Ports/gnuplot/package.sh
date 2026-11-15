#!/usr/bin/env -S bash ../.port_include.sh
port='gnuplot'
version='6.0.6'
useconfigure='true'
files=(
    "https://downloads.sourceforge.net/project/gnuplot/gnuplot/${version}/gnuplot-${version}.tar.gz#9d39d8499c9533550144092375aac45ba8bf0c18c9cee50897fe411b6d93004e"
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
