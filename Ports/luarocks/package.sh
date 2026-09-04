#!/usr/bin/env -S bash ../.port_include.sh
port='luarocks'
version='3.13.0'
useconfigure='true'
depends=(
    'lua'
    'git'
    'readline'
)
files=(
    "https://luarocks.org/releases/luarocks-${version}.tar.gz#245bf6ec560c042cb8948e3d661189292587c5949104677f1eecddc54dbe7e37"
)
installopts=("INSTALL_TOP=${SERENITY_INSTALL_ROOT}/usr/local")

configure() {
    run ./configure --with-lua-include=${SERENITY_INSTALL_ROOT}/usr/local/include --prefix=/usr/local --with-lua-interpreter=lua
}
