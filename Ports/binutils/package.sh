#!/usr/bin/env -S bash ../.port_include.sh
port=binutils
version=2.38
useconfigure=true
use_fresh_config_sub=true
configopts=("--target=${SERENITY_ARCH}-pc-serenity" "--with-sysroot=/" "--with-build-sysroot=${SERENITY_INSTALL_ROOT}" "--disable-werror" "--disable-gdb" "--disable-nls")
files="https://ftpmirror.gnu.org/gnu/binutils/binutils-${version}.tar.xz binutils-${version}.tar.xz e316477a914f567eccc34d5d29785b8b0f5a10208d36bbacedcc39048ecfe024"
auth_type="sha256"
auth_opts=("--keyring" "./gnu-keyring.gpg" "binutils-${version}.tar.xz.sig")
export ac_cv_func_getrusage=no
