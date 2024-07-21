#!/usr/bin/env -S bash ../.port_include.sh
port='stb'
version='f7f20f39fe4f206c6f19e26ebfef7b261ee59ee4'
files=(
    "https://github.com/nothings/stb/archive/${version}.zip#8b5013c681233837eec18a773b45831cea076d913c5ef8b9771204d51e8ece15"
)

build() {
    :
}

install() {
    target_dir="${SERENITY_INSTALL_ROOT}/usr/local/include/"
    run_nocd mkdir -p "${target_dir}"
    run cp -r "./" "${target_dir}"
}
