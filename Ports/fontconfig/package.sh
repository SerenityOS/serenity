#!/usr/bin/env -S bash ../.port_include.sh
port=fontconfig
version=2.13.94
useconfigure="true"
depends=("libxml2")
files="https://www.freedesktop.org/software/fontconfig/release/fontconfig-${version}.tar.xz fontconfig-${version}.tar.xz a5f052cb73fd479ffb7b697980510903b563bbb55b8f7a2b001fcfb94026003c"
auth_type="sha256"
configopts=("--prefix=/usr/local" "--enable-libxml2" "LDFLAGS=-ldl -lxml2")

export CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/libxml2"
export LIBXML2_CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/libxml2/"
export LIBXML2_LIBS="-lxml2"
