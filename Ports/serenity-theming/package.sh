#!/usr/bin/env -S bash ../.port_include.sh
port=serenity-theming
version=7ef303d19c5db6a5d5f44d8d74f52ac26d42797f
workdir="theming-${version}"
files=(
    "https://github.com/SerenityOS/theming/archive/${version}.zip 16beaca84ca0c4bcc9691129767805e6ba0f631ce1ed54f9362f0195d96d8e1b"
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
