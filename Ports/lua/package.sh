#!/usr/bin/env -S bash ../.port_include.sh
port='lua'
version='5.5.1'
files=(
    "http://www.lua.org/ftp/lua-${version}.tar.gz#1c4b4068d67061f2a2231ad2b5422e77acea1487ea9890f6320af614f4373dce"
)
depends=(
    'readline'
)
launcher_name='Lua'
launcher_category='D&evelopment'
launcher_command='/usr/local/bin/lua -i'
launcher_run_in_terminal='true'
icon_file="./doc/logo.png"
makeopts+=(
    '-Csrc/'
    'serenity'
    "CC=${CC}"
    "AR=${AR}"
    "RANLIB=${RANLIB}"
)
installopts=(
    "INSTALL_TOP=${SERENITY_INSTALL_ROOT}/usr/local"
)
