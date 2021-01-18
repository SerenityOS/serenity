#!/bin/bash ../.port_include.sh

export port=tcl
export version=8.6.11
export workdir=tcl8.6.11/unix
export useconfigure=true
export configopts="--disable-threads --disable-64bit --disable-shared"
export files="https://prdownloads.sourceforge.net/tcl/tcl8.6.11-src.tar.gz tcl8.6.11.tar.gz"

pre_configure() {
    run autoreconf -fi
}
