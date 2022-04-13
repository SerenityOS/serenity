#!/usr/bin/env -S bash ../.port_include.sh
port=fontconfig
version=2.14.0
useconfigure="true"
use_fresh_config_sub="true"
depends=("libxml2" "freetype")
files="https://www.freedesktop.org/software/fontconfig/release/fontconfig-${version}.tar.xz fontconfig-${version}.tar.xz dcbeb84c9c74bbfdb133d535fe1c7bedc9f2221a8daf3914b984c44c520e9bac"
auth_type="sha256"
configopts=("--prefix=/usr/local" "--enable-libxml2" "LDFLAGS=-ldl -lxml2")

export CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/libxml2"
export LIBXML2_CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/libxml2/"
export LIBXML2_LIBS="-lxml2"
