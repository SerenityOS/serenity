#!/usr/bin/env -S bash ../.port_include.sh
port=jot
version=6.6
files=(
    "https://github.com/ibara/libpuffy/releases/download/libpuffy-1.0/jot-${version}.tar.gz#ad7d955e6a22b5c71d32479703cdac6f2c009765e7bf1bb860775f05b1e1d303"
)
depends=("libpuffy")
