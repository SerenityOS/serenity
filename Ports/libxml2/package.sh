#!/usr/bin/env -S bash ../.port_include.sh
port=libxml2
version=2.9.13
useconfigure=true
use_fresh_config_sub=true
files="https://download.gnome.org/sources/libxml2/2.9/libxml2-${version}.tar.xz libxml2-${version}.tar.xz 276130602d12fe484ecc03447ee5e759d0465558fbc9d6bd144e3745306ebf0e"
auth_type=sha256
depends=("libiconv" "xz")
configopts=("--with-sysroot=${SERENITY_INSTALL_ROOT}" "--prefix=/usr/local" "--without-python" "--disable-static" "--enable-shared")
