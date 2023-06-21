#!/usr/bin/env -S bash ../.port_include.sh
port=pcre
version=8.45
useconfigure=true
use_fresh_config_sub=true
files="https://downloads.sourceforge.net/project/pcre/pcre/${version}/pcre-${version}.tar.gz pcre-${version}.tar.gz
https://downloads.sourceforge.net/project/pcre/pcre/${version}/pcre-${version}.tar.gz.sig pcre-${version}.tar.gz.sig"

auth_type="sig"
auth_import_key="45F68D54BBE23FB3039B46E59766E084FB0F43D8"
auth_opts=("pcre-${version}.tar.gz.sig")
