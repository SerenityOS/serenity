#!/usr/bin/env -S bash ../.port_include.sh
port='lua'
version='5.4.6'
files=(
    "http://www.lua.org/ftp/lua-${version}.tar.gz#7d5ea1b9cb6aa0b59ca3dde1c6adcb57ef83a1ba8e5432c0ecd06bf439b3ad88"
)
depends=(
    'readline'
)
launcher_name='Lua'
launcher_category='D&evelopment'
launcher_command='/usr/local/bin/lua -i'
launcher_run_in_terminal='true'
#icon_file="./doc/logo.gif" 
icon_file="https://static-00.iconduck.com/assets.00/file-type-lua-icon-256x256-l4ladwf6.png"
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
