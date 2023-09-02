#!/usr/bin/env -S bash ../.port_include.sh
port=libmad
version=0.15.1b
useconfigure=true
use_fresh_config_sub=true
use_fresh_config_guess=true
configopts=("--disable-static")
files=(
    "https://downloads.sourceforge.net/mad/libmad-${version}.tar.gz#bbfac3ed6bfbc2823d3775ebb931087371e142bb0e9bb1bee51a76a6e0078690"
)
