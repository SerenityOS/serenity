#!/usr/bin/env -S bash ../.port_include.sh
port=lua
version=5.3.5
files="http://www.lua.org/ftp/lua-${version}.tar.gz lua-${version}.tar.gz 0c2eed3f960446e1a3e4b9a1ca2f3ff893b6ce41942cf54d5dd59ab4b3b058ac"
auth_type=sha256
makeopts=("-j$(nproc)" "serenity")
installopts=("INSTALL_TOP=${SERENITY_INSTALL_ROOT}/usr/local")
