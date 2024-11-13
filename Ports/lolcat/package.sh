#!/usr/bin/env -S bash ../.port_include.sh
port='lolcat'
version='1212a9cc6c2a092359db460d9f62822b56dc02ef'
files=(
    "https://github.com/jaseg/lolcat/archive/${version}.zip#f109665a7be2cafb7e0b14438d727826396a2e65e6349266561bd60f206e355d"
)

bin_path="/usr/local/bin"

build() {
    run make
}

install() {
    run_nocd mkdir -p "${SERENITY_INSTALL_ROOT}/${bin_path}/"
    run cp lolcat "${SERENITY_INSTALL_ROOT}/${bin_path}/"
    run cp censor "${SERENITY_INSTALL_ROOT}/${bin_path}/"
}
