#!/usr/bin/env -S bash ../.port_include.sh
port=dos2unix
version=7.4.2
workdir="${port}-${version}"
files="https://waterlan.home.xs4all.nl/dos2unix/dos2unix-${version}.tar.gz ${port}-${version}.tar.gz
https://waterlan.home.xs4all.nl/dos2unix/dos2unix-7.4.2.tar.gz.asc ${port}-${version}.tar.gz.asc"
depends=("gettext")
auth_type=sig
auth_import_key="f8f1bea490496a09cca328cc38c1f572b12725be"
auth_opts=("${port}-${version}.tar.gz.asc" "${port}-${version}.tar.gz")
