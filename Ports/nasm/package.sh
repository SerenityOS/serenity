#!/usr/bin/env -S bash ../.port_include.sh
port=nasm
version=2.15.05
files="https://www.nasm.us/pub/nasm/releasebuilds/${version}/nasm-${version}.tar.gz nasm-${version}.tar.gz"
useconfigure=true
makeopts=

pre_configure() {
	run ./autogen.sh
}
