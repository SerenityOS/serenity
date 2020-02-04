#!/bin/bash ../.port_include.sh
port=flex
version=2.6.4
files="https://github.com/westes/flex/releases/download/v${version}/flex-${version}.tar.gz flex-${version}.tar.gz
https://github.com/westes/flex/releases/download/v${version}/flex-${version}.tar.gz.sig flex-${version}.tar.gz.sig"
useconfigure=true
configopts=--disable-bootstrap
depends="m4 pcre2"
auth_type="sig"
auth_import_key="E4B29C8D64885307"
auth_opts="flex-${version}.tar.gz.sig"
