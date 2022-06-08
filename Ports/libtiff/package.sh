#!/usr/bin/env -S bash ../.port_include.sh
port='libtiff'
version='4.4.0'
files="http://download.osgeo.org/libtiff/tiff-${version}.tar.xz tiff-${version}.tar.xz 49307b510048ccc7bc40f2cba6e8439182fe6e654057c1a1683139bf2ecb1dc1"
auth_type='sha256'
useconfigure='true'
configopts=(
    "--with-sysroot=${SERENITY_INSTALL_ROOT}"
    "--prefix=/usr/local"
    "--disable-static"
    "--enable-shared"
)
workdir="tiff-${version}"
depends=("libjpeg" "zstd" "xz")
