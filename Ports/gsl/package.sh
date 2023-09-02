#!/usr/bin/env -S bash ../.port_include.sh

port=gsl
version=2.7.1
useconfigure=true
files=(
    "https://ftpmirror.gnu.org/gnu/gsl/gsl-${version}.tar.gz#dcb0fbd43048832b757ff9942691a8dd70026d5da0ff85601e52687f6deeb34b"
)
use_fresh_config_sub=true
