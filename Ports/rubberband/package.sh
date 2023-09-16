#!/usr/bin/env -S bash ../.port_include.sh
port=rubberband
version=1.8.1
depends=("libfftw3f" "libsamplerate" "libsndfile")
useconfigure=true
configopts=(
    "--prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    "CFLAGS=--sysroot=${SERENITY_INSTALL_ROOT}"
)
makeopts=("-j$(nproc)" "program")
files=(
    "https://github.com/falkTX/rubberband/archive/refs/tags/v${version}.tar.gz#646685b59d4fa1b37a9dd4240c431772541468db1dee9037b9df27a0080ec4f4"
)

# pre_configure() {
#     # We patch configure.ac.
#     run autoconf
# }

post_configure() {
    run mkdir -p bin
}

export PKG_CONFIG_PATH="${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig"
