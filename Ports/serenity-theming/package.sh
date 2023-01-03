#!/usr/bin/env -S bash ../.port_include.sh
port=serenity-theming
version=7a39b7d6830aeba13ba88bb6e14a7a0d0825a370
workdir="theming-${version}"
files="https://github.com/SerenityOS/theming/archive/${version}.zip serenity-theming-${version}.zip e2b587906ce4d96a9d91f9774c204c3c82707e9d4763d6bc6b23966a6162381a"
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
    # FIXME: Update this path upstream.
    cp -r "${workdir}/color-schemes/" "${SERENITY_INSTALL_ROOT}/res/color-schemes/"
    cp -r "${workdir}/wallpapers" "${SERENITY_INSTALL_ROOT}/res/"
    cp "${workdir}/emoji-theming.txt" "${SERENITY_INSTALL_ROOT}/home/anon/Documents/emoji-theming.txt"
}
