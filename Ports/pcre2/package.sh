#!/bin/bash ../.port_include.sh
port=pcre2
version=10.34
useconfigure=true
files="https://ftp.pcre.org/pub/pcre/pcre2-${version}.tar.gz pcre2-${version}.tar.gz
https://ftp.pcre.org/pub/pcre/pcre2-${version}.tar.gz.sig pcre2-${version}.tar.gz.sig"

auth_type="sig"
auth_import_key="45F68D54BBE23FB3039B46E59766E084FB0F43D8"
auth_opts="pcre2-${version}.tar.gz.sig"
