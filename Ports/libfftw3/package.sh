#!/usr/bin/env -S bash ../.port_include.sh
port='libfftw3'
version='3.3.11'
useconfigure='true'
use_fresh_config_sub='true'
configopts=(
    '--disable-static'
    '--enable-shared'
    '--with-pic'
)
files=(
    "http://fftw.org/fftw-${version}.tar.gz#5630c24cdeb33b131612f7eb4b1a9934234754f9f388ff8617458d0be6f239a1"
)
workdir="fftw-${version}"
