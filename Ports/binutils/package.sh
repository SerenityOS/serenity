#!/usr/bin/env -S bash ../.port_include.sh
port=binutils
version=2.39
useconfigure=true
use_fresh_config_sub=true
configopts=("--target=${SERENITY_ARCH}-pc-serenity" "--with-sysroot=/" "--with-build-sysroot=${SERENITY_INSTALL_ROOT}" "--disable-werror" "--disable-gdb" "--disable-nls")
files="https://ftpmirror.gnu.org/gnu/binutils/binutils-${version}.tar.xz binutils-${version}.tar.xz 645c25f563b8adc0a81dbd6a41cffbf4d37083a382e02d5d3df4f65c09516d00"
auth_type="sha256"
auth_opts=("--keyring" "./gnu-keyring.gpg" "binutils-${version}.tar.xz.sig")
export ac_cv_func_getrusage=no
