#!/usr/bin/env -S bash ../.port_include.sh
port=tuxracer
useconfigure="true"
version="0.61"
files=(
    "http://ftp.e.kth.se/pub/mpkg/distfiles/tuxracer/${version}/tuxracer-${version}.tar.gz#a311d09080598fe556134d4b9faed7dc0c2ed956ebb10d062e5d4df022f91eff"
    "http://ftp.e.kth.se/pub/mpkg/distfiles/tuxracer/${version}/tuxracer-data-${version}.tar.gz#3783d204b7bb1ed16aa5e5a1d5944de10fbee05bc7cebb8f616fce84301f3651"
)
depends=("glu" "SDL_mixer" "sdl12-compat" "tcl")
configopts=(
    "--with-sdl-prefix=${SERENITY_INSTALL_ROOT}/usr/local"
    "--with-tcl-lib-name=tcl8.6"
    "--without-x"
)
launcher_name="Tux Racer"
launcher_category="&Games"
launcher_command="/usr/local/bin/tuxracer"

# isnan() is a macro -> not linkable
export ac_cv_func_isnan=yes

post_install() {
    resourcePath="${SERENITY_INSTALL_ROOT}/usr/local/share/tuxracer"
    mkdir -p "${resourcePath}"
    cp -r tuxracer-data-${version}/* "${resourcePath}/"
}
