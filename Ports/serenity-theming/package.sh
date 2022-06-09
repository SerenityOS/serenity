#!/usr/bin/env -S bash ../.port_include.sh
port=serenity-theming
version=22870738503a99067e1de604e28f1d0cccd461af
workdir="theming-${version}"
files="https://github.com/SerenityOS/theming/archive/${version}.zip serenity-theming-${version}.zip c6686c2c6afcb88d77454ce25e290b96295fb62f8891f26ee4bc4b204a0b9734"
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
