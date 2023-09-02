#!/usr/bin/env -S bash ../.port_include.sh
port=libmodplug
version=0.8.9.0
useconfigure=true
use_fresh_config_sub=true
configopts=("ac_cv_c_bigendian=no")
files=(
    "https://download.sourceforge.net/modplug-xmms/libmodplug-${version}.tar.gz#457ca5a6c179656d66c01505c0d95fafaead4329b9dbaa0f997d00a3508ad9de"
)
workdir="libmodplug-$version"
