#!/usr/bin/env -S bash ../.port_include.sh
port=serenity-theming
version=f6d93060b5f950a213bbc72c5aa76637b566626c
workdir="theming-${version}"
files="https://github.com/SerenityOS/theming/archive/${version}.zip serenity-theming-${version}.zip 3a891e6ccdfd783ef92813d394ec73a0c381acb15ea0e1ca080b7a9a7ce697d2"
auth_type="sha256"

build() {
    :
}

install() {
    :
}

post_install() {
    cp -r "${workdir}/icon-themes" "${SERENITY_INSTALL_ROOT}/res/"
    cp -r "${workdir}/cursor-themes" "${SERENITY_INSTALL_ROOT}/res/"
    cp -r "${workdir}/icons" "${SERENITY_INSTALL_ROOT}/res/"
    cp -r "${workdir}/themes" "${SERENITY_INSTALL_ROOT}/res/"
    cp -r "${workdir}/fonts" "${SERENITY_INSTALL_ROOT}/res/"
}
