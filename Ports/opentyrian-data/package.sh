#!/usr/bin/env -S bash ../.port_include.sh
port=opentyrian-data
description='OpenTyrian graphics and audio'
version=1.0.0
website='https://camanis.net/tyrian/tyrian21.zip'
workdir=.
files="http://camanis.net/tyrian/tyrian21.zip tyrian21.zip 7790d09a2a3addcd33c66ef063d5900eb81cc9c342f4807eb8356364dd1d9277"
auth_type=sha256

build() {
    run_nocd rm -v tyrian21/*.exe
}

install() {
    run_nocd mkdir -p ${SERENITY_INSTALL_ROOT}/usr/local/share/games/opentyrian/
    run_nocd cp -a tyrian21/* ${SERENITY_INSTALL_ROOT}/usr/local/share/games/opentyrian/
}
