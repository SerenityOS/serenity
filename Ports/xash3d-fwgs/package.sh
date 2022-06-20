#!/usr/bin/env -S bash ../.port_include.sh
port="xash3d-fwgs"
version="2022.05.01"  # Bogus version, this was the last time the commit hashes were updated.
_fwgs_commit=5402e1a2597c40c603bd0f2b1a9cd6a16506ec84
_vgui_commit=93573075afe885618ea15831e72d44bdacd65bfb
_mainui_commit=01e964fdc26f5dce1512c030d0dfd68e17be2858
_miniutl_commit=67c8c226c451f32ee3c98b94e04f8966092b70d3
useconfigure="true"
depends=("SDL2" "fontconfig" "freetype")
workdir="xash3d-fwgs-${_fwgs_commit}"
files="https://github.com/FWGS/xash3d-fwgs/archive/${_fwgs_commit}.tar.gz xash3d-fwgs-${_fwgs_commit}.tar.gz 1401f6c0cf619c48a8a40938b2acdffd327725ca0ab59804c518bddf821637f9
https://github.com/FWGS/vgui-dev/archive/${_vgui_commit}.tar.gz vgui-dev-${_vgui_commit}.tar.gz eb9315fba8ae444fdae240c10afebaf7f3b157233bf1589f0af557b2286928fa
https://github.com/FWGS/mainui_cpp/archive/${_mainui_commit}.tar.gz mainui_cpp-${_mainui_commit}.tar.gz c8f6ce81596d5690044542074ac9bc69bbd43b5e5766f71363a8b5d4d382ad71
https://github.com/FWGS/MiniUTL/archive/${_miniutl_commit}.tar.gz MiniUTL-${_miniutl_commit}.tar.gz 7b7b26377854b3fc741c8d652d8b3c9c540512644943ca6efb63df941b2861e3"
auth_type=sha256

export PKG_CONFIG_PATH="${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig"

pre_patch() {
    pushd "${workdir}"

    # Initialize submodules from tarballs
    [ -e mainui ] && rm -r mainui
    cp -r ../mainui_cpp-${_mainui_commit}/ mainui
    rmdir mainui/miniutl
    cp -r ../MiniUTL-${_miniutl_commit}/ mainui/miniutl

    popd
}

configure() {
    run ./waf configure --sdl2="${SERENITY_INSTALL_ROOT}/usr/local" --vgui=../vgui-dev-${_vgui_commit}/ -T release
}

build() {
    run ./waf build
}

install() {
    run ./waf install --destdir=${SERENITY_INSTALL_ROOT}/home/anon/Games/halflife/
}
