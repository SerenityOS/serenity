#!/usr/bin/env -S bash ../.port_include.sh

port='gsl'
version='2.8'
useconfigure='true'
files=(
    "mirror://gnu/gsl/gsl-${version}.tar.gz#6a99eeed15632c6354895b1dd542ed5a855c0f15d9ad1326c6fe2b2c9e423190"
)
use_fresh_config_sub='true'
