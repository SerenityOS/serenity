#!/usr/bin/env -S bash ../.port_include.sh
port=griffon
version="1.0"
files=(
    "https://downloads.scummvm.org/frs/extras/Griffon%20Legend/${port}-${version}.zip#0aad5fb10f51afb5c121cf04cc86539a6f0d89db85809f9e1767dfdc8d3191a4"
)
depends=("scummvm")

resource_path="/usr/local/share/games/${port}-${version}"

launcher_name="The Griffon Legend"
launcher_category='&Games'
launcher_command="/usr/local/bin/scummvm --path=${resource_path} griffon"

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
