#!/usr/bin/env -S bash ../.port_include.sh
port=libiconv
version=1.16
useconfigure=true
configopts=("--enable-shared" "--disable-nls")
files="https://ftpmirror.gnu.org/gnu/libiconv/libiconv-${version}.tar.gz libiconv-${version}.tar.gz e6a1b1b589654277ee790cce3734f07876ac4ccfaecbee8afa0b649cf529cc04"
auth_type="sha256"
use_fresh_config_sub=true
config_sub_paths=("build-aux/config.sub" "libcharset/build-aux/config.sub")
