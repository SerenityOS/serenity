#!/usr/bin/env -S bash ../.port_include.sh
port=liboggz
version=1.1.1
useconfigure=true
use_fresh_config_sub=true
files=(
    "https://downloads.xiph.org/releases/liboggz/liboggz-${version}.tar.gz#6bafadb1e0a9ae4ac83304f38621a5621b8e8e32927889e65a98706d213d415a"
)
depends=("libogg")
