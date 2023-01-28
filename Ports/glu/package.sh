#!/usr/bin/env -S bash ../.port_include.sh
port=glu
useconfigure="true"
description='Mesa GLU'
version="9.0.2"
website='https://gitlab.freedesktop.org/mesa/glu'
files="https://archive.mesa3d.org/glu/glu-${version}.tar.gz glu-${version}.tar.gz 24effdfb952453cc00e275e1c82ca9787506aba0282145fff054498e60e19a65"
auth_type=sha256
depends=("pkgconf")
use_fresh_config_sub=true

export GL_CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/include/LibGL"
export GL_LIBS="-lgl"
