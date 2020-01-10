#!/bin/bash ../.toolchain_include.sh

package=qemu
version=4.1.0
useconfigure=true
configopts="--target-list=i386-softmmu --enable-gtk"
depends="bison"

filename="qemu-${version}.tar.xz"
files="https://download.qemu.org/$filename $filename cdf2b5ca52b9abac9bacb5842fa420f8"
