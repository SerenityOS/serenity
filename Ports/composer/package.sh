#!/usr/bin/env -S bash ../.port_include.sh
port='composer'
version='2.11.2'
files=(
    "https://getcomposer.org/download/${version}/composer.phar#dd158c0afd41d15fac89e1b4e47b5d42911fe316c3687cd5edac98319b25d028"
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
