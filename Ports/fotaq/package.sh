#!/usr/bin/env -S bash ../.port_include.sh
port=fotaq
version="1.0"
files=(
    "https://downloads.scummvm.org/frs/extras/Flight%20of%20the%20Amazon%20Queen/FOTAQ_Talkie-original.zip#a298e68243f18a741d4816ef636a5a77a1593816fb2c9e23a09124c35a95dfec"
)
depends=("scummvm")

resource_path="/usr/local/share/games/${port}-${version}"

launcher_name="Flight of the Amazon Queen"
launcher_category='&Games'
launcher_command="/usr/local/bin/scummvm --path=${resource_path} queen"

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
    run_nocd cp ${workdir}/queen.1 ${target_dir}
}
