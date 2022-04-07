#!/usr/bin/env -S bash ../.port_include.sh
port=composer
version="2.3.4"
files="https://getcomposer.org/download/${version}/composer.phar composer.phar 1fc8fc5b43f081fe76fa85eb5a213412e55f54a60bae4880bc96521ae482d6c3"
auth_type=sha256
depends=("php")

build() {
    :
}

install() {
    local target_path=${SERENITY_INSTALL_ROOT}/usr/local/bin/composer
    run_nocd cp composer.phar ${target_path}
    run_nocd chmod +x ${target_path}
}
