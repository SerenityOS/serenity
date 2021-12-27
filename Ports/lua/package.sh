#!/usr/bin/env -S bash ../.port_include.sh
port=lua
version=5.3.6
files="http://www.lua.org/ftp/lua-${version}.tar.gz lua-${version}.tar.gz fc5fd69bb8736323f026672b1b7235da613d7177e72558893a0bdcd320466d60"
auth_type=sha256
makeopts=("-j$(nproc)" "serenity" "CC=${CC}" "AR=${AR}" "RANLIB=${RANLIB}")
installopts=("INSTALL_TOP=${SERENITY_INSTALL_ROOT}/usr/local")
