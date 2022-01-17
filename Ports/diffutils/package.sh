#!/usr/bin/env -S bash ../.port_include.sh
port=diffutils
version=3.7
files="https://ftpmirror.gnu.org/gnu/diffutils/diffutils-${version}.tar.xz diffutils-${version}.tar.xz b3a7a6221c3dc916085f0d205abf6b8e1ba443d4dd965118da364a1dc1cb3a26"
auth_type=sha256
useconfigure=true
use_fresh_config_sub=true
depends=("libiconv")
