#!/bin/sh ../.port_include.sh
port=lua
version=5.3.5
files="http://www.lua.org/ftp/lua-5.3.5.tar.gz lua-5.3.5.tar.gz"
makeopts="-j$(nproc) generic"
installopts="INSTALL_TOP=$SERENITY_ROOT/Root/"
