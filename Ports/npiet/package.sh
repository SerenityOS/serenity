#!/usr/bin/env -S bash ../.port_include.sh
port=npiet
useconfigure=true
version=1.3f
depends=("libgd" "libpng")
configopts=()
files=(
    "https://www.bertnase.de/npiet/npiet-${version}.tar.gz#2ded856062abd73599e85e1e768ce6bc60ba2db22dc7d6a9b62763dca04b855a"
)
