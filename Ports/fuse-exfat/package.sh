#!/usr/bin/env -S bash ../.port_include.sh
port='fuse-exfat'
version='1.4.0'
files=(
    "https://github.com/relan/exfat/releases/download/v${version}/fuse-exfat-${version}.tar.gz#a1cfedc55e0e7a12c184605aa0f0bf44b24a3fb272449b20b2c8bbe6edb3001e"
)
depends=('libfuse')
useconfigure='true'
use_fresh_config_sub='true'
