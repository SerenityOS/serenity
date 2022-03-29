#!/usr/bin/env -S bash ../.port_include.sh
port=ccleste
version=1.4.0
depends=("SDL2" "SDL2_mixer")
files="https://github.com/lemon32767/ccleste/archive/refs/tags/v${version}.tar.gz ccleste.tar.gz 32dfd797f3c863201e0c19aa97974c56a8ed589a34c0522503f25f6e1399edd6"
auth_type=sha256
launcher_name="Celeste Classic"
launcher_category=Games
launcher_command=/opt/ccleste/ccleste
icon_file=icon.png

install() {
    run make
    mkdir -p "${SERENITY_INSTALL_ROOT}/opt/ccleste"
    run cp -r ccleste data "${SERENITY_INSTALL_ROOT}/opt/ccleste"
}
