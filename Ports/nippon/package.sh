#!/usr/bin/env -S bash ../.port_include.sh
port=nippon
version="1.0"
files=(
    "https://downloads.scummvm.org/frs/extras/Nippon%20Safes/nippon-1.0.zip#53e7e2c60065e4aed193169bbcdcfd1113fa68d3efe1c8240ba073c0e20d613f"
)
depends=("scummvm")

resource_path="/usr/local/share/games/${port}-${version}"

launcher_name="Nippon Safes Inc."
launcher_category='&Games'
launcher_command="/usr/local/bin/scummvm --path=${resource_path} nippon"

build() {
    :
}

pre_fetch() {
    run_nocd mkdir -p ${workdir}
}

post_fetch() {
    run_nocd rsync -a ./* ${workdir} --exclude=${workdir} --exclude=package.sh --exclude=${port}-${version}.zip --remove-source-files
    run_nocd find . -depth -type d -empty -delete
}

install() {
    target_dir="${SERENITY_INSTALL_ROOT}${resource_path}"
    run_nocd mkdir -p ${target_dir}
    run_nocd rsync -a ${workdir}/* ${target_dir}
}
