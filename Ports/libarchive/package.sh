#!/usr/bin/env -S bash ../.port_include.sh
port=libarchive
version=3.6.1
useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("build/autoconf/config.sub")
configopts=("--without-xml2")
files=(
    "https://libarchive.org/downloads/libarchive-${version}.tar.gz#c676146577d989189940f1959d9e3980d28513d74eedfbc6b7f15ea45fe54ee2"
)
depends=("zlib" "pcre")

export ac_cv_header_regex_h=no
