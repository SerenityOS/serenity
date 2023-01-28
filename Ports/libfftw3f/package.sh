#!/usr/bin/env -S bash ../.port_include.sh
port=libfftw3f
description='Fastest Fourier Transform in the West (single precision)'
version=3.3.10
website='https://www.fftw.org/'
useconfigure=true
configopts=("--enable-float")
use_fresh_config_sub=true
files="http://fftw.org/fftw-${version}.tar.gz fftw-${version}.tar.gz 56c932549852cddcfafdab3820b0200c7742675be92179e59e6215b340e26467"
auth_type=sha256
workdir="fftw-${version}"
