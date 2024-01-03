#!/usr/bin/env -S bash ../.port_include.sh
port='freedink'
version='109.6'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=(
    'autotools/config.sub'
)
depends=(
    'fontconfig'
    'gettext'
    'glm'
    'SDL2'
    'SDL2_gfx'
    'SDL2_image'
    'SDL2_mixer'
    'SDL2_ttf'
)
freedink_data='freedink-data-1.08.20190120'
files=(
    "https://ftpmirror.gnu.org/gnu/freedink/freedink-${version}.tar.gz#5e0b35ac8f46d7bb87e656efd5f9c7c2ac1a6c519a908fc5b581e52657981002"
    "https://ftpmirror.gnu.org/gnu/freedink/${freedink_data}.tar.gz#715f44773b05b73a9ec9b62b0e152f3f281be1a1512fbaaa386176da94cffb9d"
)
configopts=(
    '--prefix=/usr/local'
    '--disable-rpath'
    '--disable-tests'
    'LDFLAGS=-ldl -lfontconfig -lxml2'
)

launcher_name='FreeDink'
launcher_category='&Games'
launcher_command='/usr/local/bin/freedink --software-rendering --truecolor --nosound'
icon_file='src/media/freedink.ico'

install() {
    run_nocd make DESTDIR="${SERENITY_INSTALL_ROOT}" -C "${freedink_data}" install
    run make DESTDIR="${SERENITY_INSTALL_ROOT}" "${installopts[@]}" install
}

export CPPFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2 -I${SERENITY_INSTALL_ROOT}/usr/local/include/libxml2"
