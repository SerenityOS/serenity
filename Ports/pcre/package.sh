#!/usr/bin/env -S bash ../.port_include.sh
port=pcre
version=8.44
useconfigure=true
files="https://ftp.pcre.org/pub/pcre/pcre-${version}.tar.gz pcre-${version}.tar.gz
https://ftp.pcre.org/pub/pcre/pcre-${version}.tar.gz.sig pcre-${version}.tar.gz.sig"

auth_type="sig"
auth_import_key="45F68D54BBE23FB3039B46E59766E084FB0F43D8"
auth_opts=("pcre-${version}.tar.gz.sig")
