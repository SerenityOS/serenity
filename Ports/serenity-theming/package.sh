#!/usr/bin/env -S bash ../.port_include.sh
port=serenity-theming
version=103b0add39f9e167e8f6c6d4715d9ffbf87d6d07
workdir="theming-${version}"
files=(
    "https://github.com/SerenityOS/theming/archive/${version}.zip serenity-theming-${version}.zip 1c3a99d961b1013dbc8b499daa3ee872c5cba29f0efb7f6f885f7f292b3dbeda"
)

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
