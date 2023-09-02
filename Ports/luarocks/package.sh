#!/usr/bin/env -S bash ../.port_include.sh
port=luarocks
version=3.8.0
useconfigure=true
depends=("lua" "git" "readline")
files=(
    "https://luarocks.org/releases/luarocks-${version}.tar.gz#56ab9b90f5acbc42eb7a94cf482e6c058a63e8a1effdf572b8b2a6323a06d923"
)

configure() {
    run ./configure --with-lua-include=${SERENITY_INSTALL_ROOT}/usr/local/include --prefix=/usr/local --with-lua-interpreter=lua
}

installopts=("INSTALL_TOP=${SERENITY_INSTALL_ROOT}/usr/local")
