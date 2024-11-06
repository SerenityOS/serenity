#!/usr/bin/env -S bash ../.port_include.sh
port='stb'
version='2e2bef463a5b53ddf8bb788e25da6b8506314c08'
files=(
    "https://github.com/nothings/stb/archive/${version}.zip#f6a4669309a29dd8634c3c2c7a955da72469c2dc61471f68d9c499e517ab823f"
)

build() {
    :
}

install() {
    target_dir="${SERENITY_INSTALL_ROOT}/usr/local/include/"
    run_nocd mkdir -p "${target_dir}"
    run cp -r "./" "${target_dir}"
}
