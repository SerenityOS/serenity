#!/bin/bash ../.port_include.sh
port=git
version=2.25.1
useconfigure="true"
files="https://mirrors.edge.kernel.org/pub/software/scm/git/git-${version}.tar.xz git-${version}.tar.xz"
configopts="--target=i686-pc-serenity"
depends="zlib"

build() {
    run make $makeopts
    run make strip
}

export NO_OPENSSL=1
export ac_cv_fread_reads_directories=no
export ac_cv_snprintf_returns_bogus=no
