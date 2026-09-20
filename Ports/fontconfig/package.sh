#!/usr/bin/env -S bash ../.port_include.sh
port='fontconfig'
version='2.16.0'
depends=(
    'freetype'
    'libxml2'
)
files=(
    "https://www.freedesktop.org/software/fontconfig/release/fontconfig-${version}.tar.xz#6a33dc555cc9ba8b10caf7695878ef134eeb36d0af366041f639b1da9b6ed220"
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

export PKG_CONFIG_PATH="${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig"
