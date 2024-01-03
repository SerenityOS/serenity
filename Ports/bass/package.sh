#!/usr/bin/env -S bash ../.port_include.sh
port=bass
version="cd-1.2"
files=(
    "https://downloads.scummvm.org/frs/extras/Beneath%20a%20Steel%20Sky/bass-${version}.zip#53209b9400eab6fd7fa71518b2f357c8de75cfeaa5ba57024575ab79cc974593"
)
depends=("scummvm")

bass_resource_path="/usr/local/share/games/${port}-${version}"

launcher_name="Beneath a Steel Sky"
launcher_category='&Games'
launcher_command="/usr/local/bin/scummvm --path=${bass_resource_path} sky"

build() {
    :
}

install() {
    target_dir="${SERENITY_INSTALL_ROOT}${bass_resource_path}"
    run_nocd mkdir -p ${target_dir}
    run_nocd cp ${workdir}/sky.{dnr,dsk} ${target_dir}
}
