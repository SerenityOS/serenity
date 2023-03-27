#!/usr/bin/env -S bash ../.port_include.sh
port='binutils'
version='2.40'
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
files="https://ftpmirror.gnu.org/gnu/binutils/binutils-${version}.tar.xz binutils-${version}.tar.xz 0f8a4c272d7f17f369ded10a4aca28b8e304828e95526da482b0ccc4dfc9d8e1"
auth_type='sha256'
auth_opts=("--keyring" "./gnu-keyring.gpg" "binutils-${version}.tar.xz.sig")
depends=("zlib")

export ac_cv_func_getrusage=no

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} "${installopts[@]}" install
    run_nocd cp ${workdir}/include/libiberty.h ${SERENITY_INSTALL_ROOT}/usr/local/include
    run_nocd cp ${workdir}/libiberty/libiberty.a ${SERENITY_INSTALL_ROOT}/usr/local/lib
}
