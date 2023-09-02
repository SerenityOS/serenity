#!/usr/bin/env -S bash ../.port_include.sh
port='xash3d-fwgs'
version='2022.12.26'  # Bogus version, this was the last time the commit hashes were updated.
_xash3d_commit='12bb0ca44b966b16c7ec3688ae07404b485ae455'
_vgui_dev_commit='93573075afe885618ea15831e72d44bdacd65bfb'
_vgui_support_commit='63c134f188e7c0891927f5a4149f4444b43b0be8'
_mainui_commit='096697124fcb7cc9720c348352b716232f80fa90'
_miniutl_commit='67c8c226c451f32ee3c98b94e04f8966092b70d3'
_opus_commit='997fdf54e781ae1c04dee42018f35388a04fe483'
_extras_commit='9aba4527435b1beda97ca8d8a5f1937cd0088c57'
useconfigure='true'
depends=("SDL2" "fontconfig" "freetype")
workdir="xash3d-fwgs-${_xash3d_commit}"
files=(
    "https://github.com/FWGS/xash3d-fwgs/archive/${_xash3d_commit}.tar.gz#71bcf9f61d05e6f9ff8866a28cdc9c644ca9aeab9e4143e279d61b1b8ebff9e5"
    "https://github.com/FWGS/vgui_support/archive/${_vgui_support_commit}.tar.gz#2f241fe093b8ab1ff757bdc4ae7a531223525ec3be8f2da3a0eddf76543e90a0"
    "https://github.com/FWGS/vgui-dev/archive/${_vgui_dev_commit}.tar.gz#eb9315fba8ae444fdae240c10afebaf7f3b157233bf1589f0af557b2286928fa"
    "https://github.com/FWGS/mainui_cpp/archive/${_mainui_commit}.tar.gz#05a3ff20055ba53d46ac65fee04a689df52889d3077661dc618f7659a2d2138f"
    "https://github.com/FWGS/MiniUTL/archive/${_miniutl_commit}.tar.gz#7b7b26377854b3fc741c8d652d8b3c9c540512644943ca6efb63df941b2861e3"
    "https://github.com/xiph/opus/archive/${_opus_commit}.tar.gz#56156f1f7a19fcd356041604ce9fdd7d70a67a0e91153f25970dcc8710ea057e"
    "https://github.com/FWGS/xash-extras/archive/${_extras_commit}.tar.gz#020b4c35f97fabbd70a7444a98451f0f5be4dcbf149e42e5e49598a5651805ab"
)

export PKG_CONFIG_PATH="${SERENITY_INSTALL_ROOT}/usr/local/lib/pkgconfig"

pre_patch() {
    pushd "${workdir}"

    # Initialize submodules from tarballs
    rm -rf 3rdparty/mainui
    cp -r ../mainui_cpp-${_mainui_commit}/ 3rdparty/mainui
    rm -rf 3rdparty/mainui/miniutl
    cp -r ../MiniUTL-${_miniutl_commit}/ 3rdparty/mainui/miniutl

    rm -rf 3rdparty/vgui_support
    cp -r ../vgui_support-${_vgui_support_commit}/ 3rdparty/vgui_support
    rm -rf 3rdparty/vgui_support/vgui-dev
    cp -r ../vgui-dev-${_vgui_dev_commit}/ 3rdparty/vgui_support/vgui-dev

    rm -rf 3rdparty/opus/opus
    cp -r ../opus-${_opus_commit}/ 3rdparty/opus/opus

    rm -rf 3rdparty/extras/xash-extras
    cp -r ../xash-extras-${_extras_commit}/ 3rdparty/extras/xash-extras

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
