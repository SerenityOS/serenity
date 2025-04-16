#!/usr/bin/env -S bash ../.port_include.sh
port='binutils'
version='2.44'
useconfigure='true'
use_fresh_config_sub='true'
configopts=(
    "--target=${SERENITY_ARCH}-pc-serenity"
    "--with-sysroot=/"
    "--with-build-sysroot=${SERENITY_INSTALL_ROOT}"
    "--disable-werror"
    "--disable-gdb"
    "--disable-nls"
    "--enable-libiberty"
)
files=(
    "https://ftpmirror.gnu.org/gnu/binutils/binutils-${version}.tar.xz#ce2017e059d63e67ddb9240e9d4ec49c2893605035cd60e92ad53177f4377237"
)
depends=(
    'zlib'
    'zstd'
)

export ac_cv_func_getrusage=no

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    run_nocd cp ${workdir}/include/libiberty.h ${SERENITY_INSTALL_ROOT}/usr/local/include
    run_nocd cp ${workdir}/libiberty/libiberty.a ${SERENITY_INSTALL_ROOT}/usr/local/lib
}
