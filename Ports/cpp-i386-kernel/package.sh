#!/bin/bash ../.port_include.sh
port=cpp-i386-kernel
version=git
workdir=cpp-i386-kernel-master
useconfigure=true
curlopts="-L"
linker="$SERENITY_ROOT/Toolchain/Local/bin/i686-pc-serenity-g++"
files="https://github.com/remyabel/cpp-i386-kernel/archive/master.tar.gz cpp-i386-kernel-git.tar.gz"
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_ROOT/Toolchain/CMakeToolchain.txt -DCMAKE_LINKER=$linker -Bbuild-output"

configure() {
    run cmake $configopts
}

build() {
    run cmake --build build-output
}

install() {
	mkdir -p $SERENITY_ROOT/Root/home/anon/cpp-i386-kernel
	cp -r cpp-i386-kernel-master/* $SERENITY_ROOT/Root/home/anon/cpp-i386-kernel
}
