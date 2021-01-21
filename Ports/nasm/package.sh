#!/usr/bin/env -S bash ../.port_include.sh
port=nasm
version=2.14.02
files="https://www.nasm.us/pub/nasm/releasebuilds/2.14.02/nasm-2.14.02.tar.gz nasm-2.14.02.tar.gz"
useconfigure=true
makeopts=

pre_configure() {
	run ./autogen.sh
}
