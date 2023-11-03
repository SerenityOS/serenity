#!/usr/bin/env -S bash ../.port_include.sh
port='composer'
version='2.6.5'
files=(
    "https://getcomposer.org/download/${version}/composer.phar#9a18e1a3aadbcb94c1bafd6c4a98ff931f4b43a456ef48575130466e19f05dd6"
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
