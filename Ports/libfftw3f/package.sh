#!/usr/bin/env -S bash ../.port_include.sh
port=libfftw3f
version=3.3.10
useconfigure=true
configopts=("--enable-float")
use_fresh_config_sub=true
files=(
    "http://fftw.org/fftw-${version}.tar.gz#56c932549852cddcfafdab3820b0200c7742675be92179e59e6215b340e26467"
)
workdir="fftw-${version}"

post_install() {
    # Some upstream libraries search for "fftw3" in pkgconfig, not "fftw3f".
    ln -sf "fftw3f.pc" "${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig/fftw3.pc"
}
