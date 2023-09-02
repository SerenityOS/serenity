#!/usr/bin/env -S bash ../.port_include.sh
port=printf
version=6.6
files=(
    "https://github.com/ibara/libpuffy/releases/download/libpuffy-1.0/printf-${version}.tar.gz#44b68af9795a3cde7dfc73a588fd2b12054dd84d1ab520106713d49935d791a8"
)
depends=("libpuffy")
