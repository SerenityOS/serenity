#!/usr/bin/env -S bash ../.port_include.sh
port=gcc
version=16.2.0
useconfigure=true
configopts=("--target=${SERENITY_ARCH}-serenity" "--with-sysroot=/" "--with-build-sysroot=${SERENITY_INSTALL_ROOT}" "--enable-languages=c,c++" "--disable-lto" "--disable-nls" "--enable-shared" "--enable-default-pie" "--enable-host-shared" "--enable-threads=posix" "--enable-initfini-array" "--with-linker-hash-style=gnu")
files=(
    "mirror://gnu/gcc/gcc-${version}/gcc-${version}.tar.xz#e6738e29597f733270731aa90600f37ffdc045079dfc27ec7e8192cc81085c3e"
)
makeopts=("all-gcc" "all-target-libgcc" "all-target-libstdc++-v3" "-j$(nproc)")
installopts=("DESTDIR=${SERENITY_INSTALL_ROOT}" "install-gcc" "install-target-libgcc" "install-target-libstdc++-v3")
depends=("binutils" "gmp" "mpfr" "mpc" "isl")


build() {
    run make "${makeopts[@]}"
    run find "./host-${SERENITY_ARCH}-serenity/gcc/" -maxdepth 1 -type f -executable -exec $STRIP --strip-debug {} \; || echo
}

install() {
    run make "${installopts[@]}"
    run ln -sf gcc "${SERENITY_INSTALL_ROOT}/usr/local/bin/cc"
    run ln -sf g++ "${SERENITY_INSTALL_ROOT}/usr/local/bin/c++"
}
