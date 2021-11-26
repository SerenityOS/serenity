#!/usr/bin/env -S bash ../.port_include.sh
port=binutils
version=2.37
useconfigure=true
configopts=("--target=${SERENITY_ARCH}-pc-serenity" "--with-sysroot=/" "--with-build-sysroot=${SERENITY_INSTALL_ROOT}" "--disable-werror" "--disable-gdb" "--disable-nls")
files="https://ftpmirror.gnu.org/gnu/binutils/binutils-${version}.tar.xz binutils-${version}.tar.xz 820d9724f020a3e69cb337893a0b63c2db161dadcb0e06fc11dc29eb1e84a32c"
auth_type="sha256"
auth_opts=("--keyring" "./gnu-keyring.gpg" "binutils-${version}.tar.xz.sig")
export ac_cv_func_getrusage=no
