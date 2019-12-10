#!/bin/bash ../.port_include.sh
port=c-ray
version=git
workdir=c-ray-master
useconfigure=true
curlopts="-L"
files="https://github.com/vkoskiv/c-ray/archive/master.tar.gz c-ray-git.tar.gz"
configopts="-DCMAKE_TOOLCHAIN_FILE=$SERENITY_ROOT/Toolchain/CMakeToolchain.txt -DNO_SDL2=True"

configure() {
    run cmake $configopts
}

install() {
	mkdir -p $SERENITY_ROOT/Root/home/anon/c-ray
	cp -r c-ray-master/* $SERENITY_ROOT/Root/home/anon/c-ray
}
