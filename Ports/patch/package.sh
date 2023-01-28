#!/usr/bin/env -S bash ../.port_include.sh
port=patch
description='patch (GNU)'
version=2.7.6
website='https://savannah.gnu.org/projects/patch/'
useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("build-aux/config.sub")
files="https://ftp.gnu.org/gnu/patch/patch-${version}.tar.gz patch-${version}.tar.gz 8cf86e00ad3aaa6d26aca30640e86b0e3e1f395ed99f189b06d4c9f74bc58a4e"
auth_type=sha256
