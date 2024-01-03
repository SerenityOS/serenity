#!/usr/bin/env -S bash ../.port_include.sh
port=lure
version="1.1"
files=(
    "https://downloads.scummvm.org/frs/extras/Lure%20of%20the%20Temptress/lure-1.1.zip#f3178245a1483da1168c3a11e70b65d33c389f1f5df63d4f3a356886c1890108"
)
depends=("scummvm")
workdir="lure"

resource_path="/usr/local/share/games/${port}-${version}"

launcher_name="Lure of the Temptress"
launcher_category='&Games'
launcher_command="/usr/local/bin/scummvm --path=${resource_path} lure"

build() {
    :
}

install() {
    target_dir="${SERENITY_INSTALL_ROOT}${resource_path}"
    run_nocd mkdir -p ${target_dir}
    run_nocd chmod 644 ${workdir}/*
    run_nocd cp ${workdir}/* ${target_dir}
}
