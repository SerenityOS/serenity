#!/usr/bin/env -S bash ../.port_include.sh
port=nasm
version=2.15.05
files="https://www.nasm.us/pub/nasm/releasebuilds/${version}/nasm-${version}.tar.gz nasm-${version}.tar.gz 9182a118244b058651c576baa9d0366ee05983c4d4ae1d9ddd3236a9f2304997"
auth_type=sha256
useconfigure=true
makeopts=()
