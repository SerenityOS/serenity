#!/usr/bin/env -S bash ../.port_include.sh
port=flex
version=2.6.4
files=(
    "https://github.com/westes/flex/releases/download/v${version}/flex-${version}.tar.gz#e87aae032bf07c26f85ac0ed3250998c37621d95f8bd748b31f15b33c45ee995"
)
useconfigure=true
use_fresh_config_sub=true
config_sub_paths=("build-aux/config.sub")
configopts=("--disable-bootstrap")
depends=("m4" "pcre2")
