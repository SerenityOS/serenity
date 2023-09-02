#!/usr/bin/env -S bash ../.port_include.sh
port=tr
version=6.7
files=(
    "https://github.com/ibara/libpuffy/releases/download/libpuffy-1.0/tr-${version}.tar.gz#6390b9f90baf097c7ee660e3d1f107161dd422e3048ce7b7bea65043b916d416"
)
depends=("libpuffy")
