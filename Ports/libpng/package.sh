#!/usr/bin/env -S bash ../.port_include.sh
port='libpng'
version='1.6.39'
useconfigure='true'
configopts=("--disable-static" "--enable-shared")
use_fresh_config_sub='true'
files=(
    "https://download.sourceforge.net/libpng/libpng-${version}.tar.gz af4fb7f260f839919e5958e5ab01a275d4fe436d45442a36ee62f73e5beb75ba"
)
depends=("zlib")
