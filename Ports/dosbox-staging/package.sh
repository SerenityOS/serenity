#!/usr/bin/env -S bash ../.port_include.sh
port='dosbox-staging'
version='0.76.0'
useconfigure='true'
configopts=(
    '--disable-fluidsynth'
    '--disable-dynamic-core'
    '--disable-opus-cdda'
)
files="https://github.com/dosbox-staging/dosbox-staging/archive/refs/tags/v${version}.tar.gz v${version}.tar.gz 7df53c22f7ce78c70afb60b26b06742b90193b56c510219979bf12e0bb2dc6c7"
auth_type='sha256'
depends=(
    'libpng'
    'SDL2'
)
launcher_name='DOSBox'
launcher_category='Games'
launcher_command='/usr/local/bin/dosbox'
icon_file='contrib/icons/dosbox-staging.ico'
use_fresh_config_sub='true'

export CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2"
export CPPFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2"

pre_patch() {
    run ./autogen.sh
}
