#!/usr/bin/env -S bash ../.port_include.sh
port=imgcat
version=2.5.1
useconfigure=true
installopts=("PREFIX=${SERENITY_INSTALL_ROOT}/usr/local")
depends=("ncurses" "libpng" "libjpeg")
files=(
    "https://github.com/eddieantonio/imgcat/releases/download/v${version}/imgcat-${version}.tar.gz#8faaac392df315b4973bb6927c0eec659e879df6c15ad6f8461073e05b70c537"
)
