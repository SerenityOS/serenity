#!/usr/bin/env -S bash ../.port_include.sh
port=diffutils
description='GNU Diffutils'
version=3.8
website='https://www.gnu.org/software/diffutils/'
files="https://ftpmirror.gnu.org/gnu/diffutils/diffutils-${version}.tar.xz diffutils-${version}.tar.xz a6bdd7d1b31266d11c4f4de6c1b748d4607ab0231af5188fc2533d0ae2438fec"
auth_type=sha256
useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("build-aux/config.sub")
depends=("libiconv")
