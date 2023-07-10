#!/usr/bin/env -S bash ../.port_include.sh
port='tar'
version='1.34'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('build-aux/config.sub')
files=(
    "https://ftpmirror.gnu.org/gnu/tar/tar-${version}.tar.gz tar-${version}.tar.gz 03d908cf5768cfe6b7ad588c921c6ed21acabfb2b79b788d1330453507647aed"
)
configopts=(
    "--without-selinux"
    "--without-posix-acls"
    "--without-xattrs"
)
