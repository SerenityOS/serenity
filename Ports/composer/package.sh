#!/usr/bin/env -S bash ../.port_include.sh
port=composer
version="2.1.3"
files="https://getcomposer.org/download/${version}/composer.phar composer.phar f8a72e98dec8da736d8dac66761ca0a8fbde913753e9a43f34112367f5174d11"
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
