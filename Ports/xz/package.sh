#!/usr/bin/env -S bash ../.port_include.sh
port=xz
version=5.4.1
useconfigure=true
configopts=("--disable-static" "--enable-shared")
use_fresh_config_sub=true
config_sub_paths=("build-aux/config.sub")
files="https://tukaani.org/xz/xz-${version}.tar.gz xz-${version}.tar.gz e4b0f81582efa155ccf27bb88275254a429d44968e488fc94b806f2a61cd3e22"
auth_type=sha256
depends=("zlib" "libiconv")
