#!/usr/bin/env -S bash ../.port_include.sh
port=sfinx
version="1.1"
files=(
    "https://downloads.scummvm.org/frs/extras/Sfinx/sfinx-en-v1.1.zip#f516b30a046526f78cbc923d8f907d267ab964ccd9b770afc72350e8d467ec4d"
)
depends=("scummvm")
workdir="${port}-en-v${version}"

resource_path="/usr/local/share/games/${port}-${version}"

launcher_name="Sfinx"
launcher_category="&Games"
launcher_command="/usr/local/bin/scummvm --path=${resource_path} sfinx"

build() {
    :
}

install() {
    target_dir="${SERENITY_INSTALL_ROOT}${resource_path}"
    run_nocd mkdir -p ${target_dir}
    run_nocd cp ${workdir}/* ${target_dir}
}
