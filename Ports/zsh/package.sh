#!/usr/bin/env -S bash ../.port_include.sh
port=zsh
description='Z Shell (Zsh)'
version=5.8.1
website='https://www.zsh.org'
files="https://sourceforge.net/projects/zsh/files/zsh/${version}/zsh-${version}.tar.xz zsh-${version}.tar.xz b6973520bace600b4779200269b1e5d79e5f505ac4952058c11ad5bbf0dd9919"
auth_type="sha256"
useconfigure=true
use_fresh_config_sub=true

pre_configure() {
    run "./Util/preconfig"
}
