#!/usr/bin/env -S bash ../.port_include.sh
port=tuxracer
useconfigure="true"
version="0.61"
files="http://ftp.e.kth.se/pub/mpkg/distfiles/tuxracer/${version}/tuxracer-${version}.tar.gz tuxracer-${version}.tar.gz a311d09080598fe556134d4b9faed7dc0c2ed956ebb10d062e5d4df022f91eff
http://ftp.e.kth.se/pub/mpkg/distfiles/tuxracer/${version}/tuxracer-data-${version}.tar.gz tuxracer-data-${version}.tar.gz 3783d204b7bb1ed16aa5e5a1d5944de10fbee05bc7cebb8f616fce84301f3651"
auth_type=sha256
depends=("glu" "SDL2" "SDL2_mixer" "tcl")
configopts=(
    "--with-gl-inc=${SERENITY_INSTALL_ROOT}/usr/include/LibGL"
    "--with-gl-lib-name=gl"
    "--with-sdl-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    "--with-tcl-lib-name=tcl8.6"
    "--without-x"
)
launcher_name="Tux Racer"
launcher_category="Games"
launcher_command="/usr/local/bin/tuxracer"

pre_configure() {
    export CFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2"
    export CXXFLAGS="-I${SERENITY_INSTALL_ROOT}/usr/local/include/SDL2"
    export LIBS="-lSDL2"
}

post_configure() {
    unset LIBS
    unset CXXFLAGS
    unset CFLAGS
}

post_install() {
    resourcePath="${SERENITY_INSTALL_ROOT}/usr/local/share/tuxracer"
    mkdir -p "${resourcePath}"
    cp -r tuxracer-data-${version}/* "${resourcePath}/"
}
