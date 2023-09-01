#!/usr/bin/env -S bash ../.port_include.sh
port='composer'
version='2.5.8'
files=(
    "https://getcomposer.org/download/${version}/composer.phar#f07934fad44f9048c0dc875a506cca31cc2794d6aebfc1867f3b1fbf48dce2c5"
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
