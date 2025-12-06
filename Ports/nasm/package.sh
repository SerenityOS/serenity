#!/usr/bin/env -S bash ../.port_include.sh
port='nasm'
version='3.01'
files=(
    "https://www.nasm.us/pub/nasm/releasebuilds/${version}/nasm-${version}.tar.gz#aea120d4adb0241f08ae24d6add09e4a993bc1c4d9f754dbfc8020d6916c9be1"
)
useconfigure=true
makeopts=()
