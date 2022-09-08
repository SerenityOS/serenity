#!/usr/bin/env -S bash ../.port_include.sh
port=serenity-theming
version=3b4e1f2434e10b63f7b4cc6dc520e69de32671b2
workdir="theming-${version}"
files="https://github.com/SerenityOS/theming/archive/${version}.zip serenity-theming-${version}.zip 9c5cc4600f39d7f19bb1f3a7575bbc844d005aa8901e28073bc5830964542fba"
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
    cp -r "${workdir}/color-palettes" "${SERENITY_INSTALL_ROOT}/res/"
    cp -r "${workdir}/emoji" "${SERENITY_INSTALL_ROOT}/res/"
    cp -r "${workdir}/terminal-colors" "${SERENITY_INSTALL_ROOT}/res/"
    cp -r "${workdir}/wallpapers" "${SERENITY_INSTALL_ROOT}/res/"
}
