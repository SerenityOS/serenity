#!/usr/bin/env -S bash ../.port_include.sh
port=gcc
version=13.2.0
useconfigure=true
configopts=("--target=${SERENITY_ARCH}-pc-serenity" "--with-sysroot=/" "--with-build-sysroot=${SERENITY_INSTALL_ROOT}" "--enable-languages=c,c++" "--disable-lto" "--disable-nls" "--enable-shared" "--enable-default-pie" "--enable-host-shared" "--enable-threads=posix" "--enable-initfini-array" "--with-linker-hash-style=gnu")
files=(
    "https://ftpmirror.gnu.org/gnu/gcc/gcc-${version}/gcc-${version}.tar.xz#e275e76442a6067341a27f04c5c6b83d8613144004c0413528863dc6b5c743da"
)
makeopts=("all-gcc" "all-target-libgcc" "all-target-libstdc++-v3" "-j$(nproc)")
installopts=("DESTDIR=${SERENITY_INSTALL_ROOT}" "install-gcc" "install-target-libgcc" "install-target-libstdc++-v3")
depends=("binutils" "gmp" "mpfr" "mpc" "isl")


build() {
    run make "${makeopts[@]}"
    run find "./host-${SERENITY_ARCH}-pc-serenity/gcc/" -maxdepth 1 -type f -executable -exec $STRIP --strip-debug {} \; || echo
}

install() {
    run make "${installopts[@]}"
    run ln -sf gcc "${SERENITY_INSTALL_ROOT}/usr/local/bin/cc"
    run ln -sf g++ "${SERENITY_INSTALL_ROOT}/usr/local/bin/c++"
}
