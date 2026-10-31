#!/usr/bin/env -S bash ../.port_include.sh
port='composer'
version='2.11.1'
files=(
    "https://getcomposer.org/download/${version}/composer.phar#6f1b891e1d4927d62740276c540fb490e47689e19e102c49e8da849794882f8c"
)
depends=(
    'php'
)

build() {
    :
}

install() {
    local target_path="${SERENITY_INSTALL_ROOT}/usr/local/bin/composer"
    run_nocd cp composer.phar "${target_path}"
    run_nocd chmod +x "${target_path}"
}
