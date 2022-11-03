#!/usr/bin/env -S bash ../.port_include.sh
port=serenity-theming
version=490a15af05e378f57891a2dc43178fdc9d4442a0
workdir="theming-${version}"
files="https://github.com/SerenityOS/theming/archive/${version}.zip serenity-theming-${version}.zip 09ee982972ccf4d6ee65361a7c54f946b3bba2c02a2296367e602fe154cd4c9a"
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
    cp "${workdir}/emoji-theming.txt" "${SERENITY_INSTALL_ROOT}/home/anon/Documents/emoji-theming.txt"
}
