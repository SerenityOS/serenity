#!/usr/bin/env -S bash ../.port_include.sh
port=libarchive
version=3.5.2
useconfigure=true
use_fresh_config_sub=true
configopts=("--without-xml2")
files="https://libarchive.org/downloads/libarchive-${version}.tar.gz libarchive-${version}.tar.gz
https://libarchive.org/downloads/libarchive-${version}.tar.gz.asc libarchive-${version}.tar.gz.asc"
depends=("zlib" "pcre")
auth_type="sig"
auth_import_key="A5A45B12AD92D964B89EEE2DEC560C81CEC2276E"
auth_opts=("libarchive-${version}.tar.gz.asc" "libarchive-${version}.tar.gz")

export ac_cv_header_regex_h=no
