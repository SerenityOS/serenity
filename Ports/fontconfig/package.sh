#!/usr/bin/env -S bash ../.port_include.sh
port='fontconfig'
version='2.14.2'
depends=(
    'freetype'
    'libxml2'
)
files=(
    "https://www.freedesktop.org/software/fontconfig/release/fontconfig-${version}.tar.xz#dba695b57bce15023d2ceedef82062c2b925e51f5d4cc4aef736cf13f60a468b"
)
useconfigure='true'
use_fresh_config_sub='true'
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
    '--prefix=/usr/local'
    '--disable-static'
    '--enable-shared'
    '--enable-libxml2'
    'LDFLAGS=-ldl -lxml2'
)

export CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/libxml2"
export LIBXML2_CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/libxml2/"
export LIBXML2_LIBS='-lxml2'
