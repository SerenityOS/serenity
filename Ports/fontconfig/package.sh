#!/usr/bin/env -S bash ../.port_include.sh
port=fontconfig
version=2.14.0
useconfigure="true"
use_fresh_config_sub="true"
depends=("libxml2" "freetype")
files=(
    "https://www.freedesktop.org/software/fontconfig/release/fontconfig-${version}.tar.xz dcbeb84c9c74bbfdb133d535fe1c7bedc9f2221a8daf3914b984c44c520e9bac"
)
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
    "--prefix=/usr/local"
    "--disable-static"
    "--enable-shared"
    "--enable-libxml2"
    "LDFLAGS=-ldl -lxml2"
)

export CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/libxml2"
export LIBXML2_CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/libxml2/"
export LIBXML2_LIBS="-lxml2"
