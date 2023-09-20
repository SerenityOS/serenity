#!/usr/bin/env -S bash ../.port_include.sh
port='libfftw3'
version='3.3.10'
useconfigure='true'
use_fresh_config_sub='true'
configopts=(
    '--disable-static'
    '--enable-shared'
    '--with-pic'
)
files=(
    "http://fftw.org/fftw-${version}.tar.gz#56c932549852cddcfafdab3820b0200c7742675be92179e59e6215b340e26467"
)
workdir="fftw-${version}"
