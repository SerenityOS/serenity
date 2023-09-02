#!/usr/bin/env -S bash ../.port_include.sh
port=gdb
version=11.2
useconfigure=true
configopts=("--target=${SERENITY_ARCH}-pc-serenity" "--with-sysroot=/" "--with-build-sysroot=${SERENITY_INSTALL_ROOT}" "--with-newlib" "--enable-languages=c,c++" "--disable-lto" "--disable-nls" "--enable-shared" "--enable-default-pie" "--enable-host-shared" "--enable-threads=posix")
files=(
    "https://ftpmirror.gnu.org/gnu/gdb/gdb-${version}.tar.xz#1497c36a71881b8671a9a84a0ee40faab788ca30d7ba19d8463c3cc787152e32"
)
makeopts+=("all")
installopts=("DESTDIR=${SERENITY_INSTALL_ROOT}")
depends=("gmp" "binutils")

# We only have a stub of getrusage(..)
export ac_cv_func_getrusage=no

# We don't support the madvise options that are used.
export ac_cv_func_madvise=no
