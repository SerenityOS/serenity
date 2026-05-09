#!/usr/bin/env -S bash ../.port_include.sh
port='binutils'
version='2.46.0'
useconfigure='true'
use_fresh_config_sub='true'
configopts=(
    "--target=${SERENITY_ARCH}-serenity"
    "--with-sysroot=/"
    "--with-build-sysroot=${SERENITY_INSTALL_ROOT}"
    "--disable-werror"
    "--disable-gdb"
    "--disable-nls"
    "--enable-libiberty"
)
files=(
    "https://ftpmirror.gnu.org/gnu/binutils/binutils-${version}.tar.xz#d75a94f4d73e7a4086f7513e67e439e8fcdcbb726ffe63f4661744e6256b2cf2"
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
