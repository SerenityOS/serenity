#!/usr/bin/env -S bash ../.port_include.sh
port=soltys
version="1.0"
files=(
    "https://downloads.scummvm.org/frs/extras/Soltys/soltys-en-v1.0.zip#87b89e654b8a5b8ebe342cb4c5c6049ab9a43a5efb474d9c49bafb77dcce48f6"
)
depends=("scummvm")

resource_path="/usr/local/share/games/${port}-${version}"

launcher_name="Soltys"
launcher_category='&Games'
launcher_command="/usr/local/bin/scummvm --path=${resource_path} soltys"

build() {
    :
}

pre_fetch() {
    run_nocd mkdir -p ${workdir}
}

post_fetch() {
    run_nocd rsync -a ./* ${workdir} --exclude=package.sh --exclude=${workdir} --exclude=${port}-en-v${version}.zip --remove-source-files
    run_nocd find . -depth -type d -empty -delete
}

install() {
    target_dir="${SERENITY_INSTALL_ROOT}${resource_path}"
    run_nocd mkdir -p ${target_dir}
    run_nocd cp ${workdir}/* ${target_dir}
}
