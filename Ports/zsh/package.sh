#!/usr/bin/env -S bash ../.port_include.sh
port=zsh
version=5.8
useconfigure=true
files="https://www.zsh.org/pub/zsh-${version}.tar.xz zsh-${version}.tar.xz
https://www.zsh.org/pub/zsh-${version}.tar.xz.asc zsh-${version}.tar.xz.asc"

auth_type="sig"
auth_import_key="7CA7ECAAF06216B90F894146ACF8146CAE8CBBC4"
auth_opts="zsh-${version}.tar.xz.asc"

post_install() {
    mkdir -p "${SERENITY_BUILD_DIR}/Root/bin"
    ln -s /usr/local/bin/zsh "${SERENITY_BUILD_DIR}/Root/bin/zsh"
}

post_uninstall() {
  rm ${SERENITY_BUILD_DIR}/Root/bin/zsh
}
