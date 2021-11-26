#!/usr/bin/env -S bash ../.port_include.sh
port=imgcat
version=2.5.0
depends=("ncurses")
files="https://github.com/eddieantonio/imgcat/releases/download/v${version}/imgcat-${version}.tar.gz imgcat-v${version}.tar.gz 8f18e10464ed1426b29a5b11aee766a43db92be17ba0a17fd127dd9cf9fb544b"
auth_type=sha256

build() {
    run make \
        production=true
}

export CPPFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/ncurses"
