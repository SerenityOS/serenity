#!/usr/bin/env -S bash ../.port_include.sh
port='gdb'
version='18.1'
useconfigure='true'
configopts=("--target=${SERENITY_ARCH}-serenity" "--with-sysroot=/" "--with-build-sysroot=${SERENITY_INSTALL_ROOT}" "--with-newlib" "--enable-languages=c,c++" "--disable-lto" "--disable-nls" "--enable-shared" "--enable-default-pie" "--enable-host-shared" "--enable-threads=posix")
files=(
    "mirror://gnu/gdb/gdb-${version}.tar.xz#cd9fc3fe2b47743840e42c1592d3d87f8302eb18639c0b8b4ba0898002e2348f"
)
makeopts+=("all")
installopts=("DESTDIR=${SERENITY_INSTALL_ROOT}")
depends=(
    'binutils' # TODO: What is this needed for?
    'gmp'
    'mpfr'
)

# We only have a stub of getrusage(..)
export ac_cv_func_getrusage=no

# We don't support the madvise options that are used.
export ac_cv_func_madvise=no
