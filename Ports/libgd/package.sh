#!/usr/bin/env -S bash ../.port_include.sh
port=libgd
version=2.3.3
useconfigure=true
use_fresh_config_sub=true
config_sub_path=build-aux/config.sub
files="https://github.com/libgd/libgd/releases/download/gd-${version}/libgd-${version}.tar.gz libgd-${version}.tar.gz dd3f1f0bb016edcc0b2d082e8229c822ad1d02223511997c80461481759b1ed2"
auth_type=sha256
depends=("libpng")
