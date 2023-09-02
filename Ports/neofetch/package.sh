#!/usr/bin/env -S bash ../.port_include.sh

port=neofetch
version=7.1.0
useconfigure=false
depends=("bash" "jq")
files=(
    "https://github.com/dylanaraps/neofetch/archive/${version}.tar.gz#58a95e6b714e41efc804eca389a223309169b2def35e57fa934482a6b47c27e7"
)

install() {
    run make DESTDIR=${SERENITY_INSTALL_ROOT} PREFIX=/usr/local "${installopts[@]}" install
}
