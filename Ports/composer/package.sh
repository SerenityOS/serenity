#!/usr/bin/env -S bash ../.port_include.sh
port='composer'
version='2.3.5'
files="https://getcomposer.org/download/${version}/composer.phar composer.phar 3b3b5a899c06a46aec280727bdf50aad14334f6bc40436ea76b07b650870d8f4"
auth_type='sha256'
depends=("php")

build() {
    :
}

install() {
    local target_path="${SERENITY_INSTALL_ROOT}/usr/local/bin/composer"
    run_nocd cp composer.phar "${target_path}"
    run_nocd chmod +x "${target_path}"
}
