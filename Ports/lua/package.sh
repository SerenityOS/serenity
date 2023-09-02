#!/usr/bin/env -S bash ../.port_include.sh
port='lua'
version='5.4.4'
files=(
    "http://www.lua.org/ftp/lua-${version}.tar.gz#164c7849653b80ae67bec4b7473b884bf5cc8d2dca05653475ec2ed27b9ebf61"
)
depends=("readline")
makeopts=(
    "-Csrc/"
    "-j$(nproc)"
    "serenity"
    "CC=${CC}"
    "AR=${AR}"
    "RANLIB=${RANLIB}"
)
installopts=(
    "INSTALL_TOP=${SERENITY_INSTALL_ROOT}/usr/local"
)
