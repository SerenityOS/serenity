#!/usr/bin/env -S bash ../.port_include.sh
port='lzop'
version='1.04'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('autoconf/config.sub')
files=(
    "https://www.lzop.org/download/lzop-${version}.tar.gz#7e72b62a8a60aff5200a047eea0773a8fb205caf7acbe1774d95147f305a2f41"
)
depends=("lzo")
