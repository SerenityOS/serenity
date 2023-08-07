#!/usr/bin/env -S bash ../.port_include.sh
port='composer'
version='2.4.3'
files=(
    "https://getcomposer.org/download/${version}/composer.phar 26d72f2790502bc9b22209e1cec1e0e43d33b368606ad227d327cccb388b609a"
)
depends=('php')

build() {
    :
}

install() {
    local target_path="${SERENITY_INSTALL_ROOT}/usr/local/bin/composer"
    run_nocd cp composer.phar "${target_path}"
    run_nocd chmod +x "${target_path}"
}
