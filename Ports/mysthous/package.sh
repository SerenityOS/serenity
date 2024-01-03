#!/usr/bin/env -S bash ../.port_include.sh
port=mysthous
version="1.0"
files=(
    "https://downloads.scummvm.org/frs/extras/Mystery%20House/MYSTHOUS.zip#ada412228a149394489b28c6c7f9ebab0722b52e04732fd0aa22949673cfa3a0"
)
depends=("scummvm")

resource_path="/usr/local/share/games/${port}-${version}"

launcher_name="Hi-Res Adventure #1: Mystery House"
launcher_category='&Games'
launcher_command="/usr/local/bin/scummvm --path=${resource_path} hires1-apple2"

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
    run_nocd chmod 644 ${workdir}/*
    run_nocd cp ${workdir}/MYSTHOUS.DSK ${target_dir}
}
