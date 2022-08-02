#!/usr/bin/env -S bash ../.port_include.sh
port='quake'
version='0.65'
workdir="tyrutils-${version}"
useconfigure='false'
auth_type='sha256'
files="https://disenchant.net/files/engine/tyrquake-${version}.tar.gz tyrquake-${version}.tar.gz 6636137d9f8fe6656f0b1110935df485c2034c11cfebd80b060ddcd4abcd0081"
makeopts=(
    'V=1'
    'SYMBOLS_ON=N'
    'TARGET_OS=UNIX'
    'TARGET_UNIX=serenity'
    'USE_SDL=Y'
    "BIN_DIR=${SERENITY_INSTALL_ROOT}/opt/quake"
    'CD_TARGET=null'
)
depends=('SDL2')
launcher_name='Quake'
launcher_category='Games'
launcher_command='/opt/quake/tyr-glquake -basedir /opt/quake'
icon_file='icons/tyrquake-1024x1024.png'

install() {
    echo 'Fake override, there is no install target to run'
}

post_install() {
    echo -e '\033[0;34m======================================================================================\033[0m'
    echo -e '\033[1;31mNOTE: \033[0mThis Quake port needs the assets from the original game to work.'
    echo -e 'Place the \033[1;33mpak0.pak\033[0m file (shareware) or both pak0.pak and \033[1;33mpak1.pak\033[0m'
    echo -e "(full version) under \033[1;32m${SERENITY_INSTALL_ROOT}/opt/quake/id1/\033[0m."
    echo -e '\033[0;34m======================================================================================\033[0m'    
}
