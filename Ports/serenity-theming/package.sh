#!/usr/bin/env -S bash ../.port_include.sh
port=serenity-theming
version=49d499b675a397c30e3401ec98927157324e2762
workdir="theming-${version}"
files="https://github.com/SerenityOS/theming/archive/${version}.zip serenity-theming-${version}.zip c7fcb9b200fb4639fde9439075cd87624e5031d8e4353f297c65261431fe8a27"
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
    cp -r "${workdir}/graphics" "${SERENITY_INSTALL_ROOT}/res/"
    cp -r "${workdir}/color-schemes" "${SERENITY_INSTALL_ROOT}/res/color-schemes/"
    cp -r "${workdir}/wallpapers" "${SERENITY_INSTALL_ROOT}/res/"
    cp "${workdir}/emoji-theming.txt" "${SERENITY_INSTALL_ROOT}/home/anon/Documents/emoji-theming.txt"
}
