#!/usr/bin/env -S bash ../.port_include.sh
port=pcre2
version=10.39
useconfigure=true
files="https://github.com/PhilipHazel/pcre2/releases/download/pcre2-${version}/pcre2-${version}.tar.gz pcre2-${version}.tar.gz
https://github.com/PhilipHazel/pcre2/releases/download/pcre2-${version}/pcre2-${version}.tar.gz.sig pcre2-${version}.tar.gz.sig"

auth_type="sig"
auth_import_key="45F68D54BBE23FB3039B46E59766E084FB0F43D8"
auth_opts=("pcre2-${version}.tar.gz.sig")
