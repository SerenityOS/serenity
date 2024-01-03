#!/usr/bin/env -S bash ../.port_include.sh
port=drascula
version="1.0"
files=(
    "https://downloads.scummvm.org/frs/extras/Drascula_%20The%20Vampire%20Strikes%20Back/drascula-1.0.zip#b731f6cb5a22ba8b4c3b3362f570b9a10a67b6cb0b395394b19a94b36e4e42de"
)
depends=("scummvm")

resource_path="/usr/local/share/games/${port}-${version}"

launcher_name="Dráscula: The Vampire Strikes Back"
launcher_category='&Games'
launcher_command="/usr/local/bin/scummvm --path=${resource_path} drascula"

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
    run_nocd cp ${workdir}/Packet.001 ${target_dir}
}
