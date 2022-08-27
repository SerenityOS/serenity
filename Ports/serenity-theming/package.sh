#!/usr/bin/env -S bash ../.port_include.sh
port=serenity-theming
version=624c29a22d11fa01ea6729afbd96375d809cc99b
workdir="theming-${version}"
files="https://github.com/SerenityOS/theming/archive/${version}.zip serenity-theming-${version}.zip a043589b82b52dbc7ad8f819f21955e51b4358196ee9efc6942720dd912662b7"
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
