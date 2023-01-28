#!/usr/bin/env -S bash ../.port_include.sh
port=yasm
description='Yasm Modular Assembler'
version=1.3.0
website='https://yasm.tortall.net/'
files="https://www.tortall.net/projects/yasm/releases/yasm-${version}.tar.gz yasm-${version}.tar.gz 3dce6601b495f5b3d45b59f7d2492a340ee7e84b5beca17e48f862502bd5603f"
auth_type="sha256"
useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("config/config.sub")
makeopts=()
