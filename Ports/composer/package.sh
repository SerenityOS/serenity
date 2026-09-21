#!/usr/bin/env -S bash ../.port_include.sh
port='composer'
version='2.10.3'
files=(
    "https://getcomposer.org/download/${version}/composer.phar#7a2d379d5b8ffdaa028580ef26494c36d2feef4b178d3dd1473a4dbc5e17c8d6"
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
