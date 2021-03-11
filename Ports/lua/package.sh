#!/usr/bin/env -S bash ../.port_include.sh
port=lua
version=5.3.5
files="http://www.lua.org/ftp/lua-${version}.tar.gz lua-${version}.tar.gz 4f4b4f323fd3514a68e0ab3da8ce3455"
makeopts="-j$(nproc) serenity"
installopts="INSTALL_TOP=${SERENITY_BUILD_DIR}/Root/usr/local"
