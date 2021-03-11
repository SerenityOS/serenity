#!/usr/bin/env -S bash ../.port_include.sh
port=git
version=2.26.0
useconfigure="true"
files="https://mirrors.edge.kernel.org/pub/software/scm/git/git-${version}.tar.xz git-${version}.tar.xz"
configopts="--target=${SERENITY_ARCH}-pc-serenity CFLAGS=-DNO_IPV6"
depends="zlib"

build() {
    run make $makeopts
    run make strip
}

post_install() {
    run mkdir -p "${SERENITY_BUILD_DIR}/Root/home/anon"
    run cp "../default_gitconfig" "${SERENITY_BUILD_DIR}/Root/home/anon/.gitconfig"
}

export NO_OPENSSL=1
export ac_cv_fread_reads_directories=no
export ac_cv_snprintf_returns_bogus=no
