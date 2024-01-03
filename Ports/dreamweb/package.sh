#!/usr/bin/env -S bash ../.port_include.sh
port=dreamweb
version="1.1"
files=(
    "https://downloads.scummvm.org/frs/extras/Dreamweb/dreamweb-cd-uk-1.1.zip#4a6f13911ce67d62c526e41048ec067b279f1b378c9210f39e0ce8d3f2b80142"
)
depends=("scummvm")

resource_path="/usr/local/share/games/${port}-${version}"

launcher_name="DreamWeb"
launcher_category='&Games'
launcher_command="/usr/local/bin/scummvm --path=${resource_path} dreamweb"

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
