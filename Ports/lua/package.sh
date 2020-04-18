#!/bin/bash ../.port_include.sh
port=lua
version=5.3.5
files="http://www.lua.org/ftp/lua-5.3.5.tar.gz lua-5.3.5.tar.gz 4f4b4f323fd3514a68e0ab3da8ce3455"
makeopts="-j$(nproc) serenity"
installopts="INSTALL_TOP=$SERENITY_ROOT/Build/Root/usr/local"
