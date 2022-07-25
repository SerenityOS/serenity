#!/usr/bin/env -S bash ../.port_include.sh
port='xash3d-fwgs'
version='2022.07.14'  # Bogus version, this was the last time the commit hashes were updated.
_fwgs_commit='772f4dcb60a8a2594df40195af4a0d4bd8ea7863'
_vgui_dev_commit='93573075afe885618ea15831e72d44bdacd65bfb'
_vgui_support_commit='991085982209a1b8eefabae04d842004d4f4fe4f'
_mainui_commit='97fcbf8979f22d774b1cc01cb5553743592d39d0'
_miniutl_commit='67c8c226c451f32ee3c98b94e04f8966092b70d3'
useconfigure='true'
depends=("SDL2" "fontconfig" "freetype")
workdir="xash3d-fwgs-${_fwgs_commit}"
files="https://github.com/FWGS/xash3d-fwgs/archive/${_fwgs_commit}.tar.gz xash3d-fwgs-${_fwgs_commit}.tar.gz 0840c08f107c63bd54f75ccb49ce338c2b1c9532adbab833dd53a56ff896432b
https://github.com/FWGS/vgui_support/archive/${_vgui_support_commit}.tar.gz vgui_support-${_vgui_support_commit}.tar.gz 68ac969310faea7b47d78f114039a78f3ee79909365cb998b57cc717c51bb871
https://github.com/FWGS/vgui-dev/archive/${_vgui_dev_commit}.tar.gz vgui-dev-${_vgui_dev_commit}.tar.gz eb9315fba8ae444fdae240c10afebaf7f3b157233bf1589f0af557b2286928fa
https://github.com/FWGS/mainui_cpp/archive/${_mainui_commit}.tar.gz mainui_cpp-${_mainui_commit}.tar.gz 9b8b469c8f0e23f6f62da91234517caf18b944217284d4eb8a506b5462460222
https://github.com/FWGS/MiniUTL/archive/${_miniutl_commit}.tar.gz MiniUTL-${_miniutl_commit}.tar.gz 7b7b26377854b3fc741c8d652d8b3c9c540512644943ca6efb63df941b2861e3"
auth_type='sha256'

export PKG_CONFIG_PATH="${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig"

pre_patch() {
    pushd "${workdir}"

    # Initialize submodules from tarballs
    [ -e mainui ] && rm -r mainui
    cp -r ../mainui_cpp-${_mainui_commit}/ mainui
    rmdir mainui/miniutl
    cp -r ../MiniUTL-${_miniutl_commit}/ mainui/miniutl

    rm -rf vgui_support
    cp -r ../vgui_support-${_vgui_support_commit}/ vgui_support
    rm -rf vgui_support/vgui-dev
    cp -r ../vgui-dev-${_vgui_dev_commit}/ vgui_support/vgui-dev

    popd
}

configure() {
    run ./waf configure --sdl2="${SERENITY_INSTALL_ROOT}/usr/local" -T release
}

build() {
    run ./waf build
}

install() {
    run ./waf install --destdir=${SERENITY_INSTALL_ROOT}/home/anon/Games/halflife/
}
