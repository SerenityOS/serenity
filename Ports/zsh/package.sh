#!/usr/bin/env -S bash ../.port_include.sh
port=zsh
version=5.8
files="https://sourceforge.net/projects/zsh/files/zsh/${version}/zsh-${version}.tar.xz zsh-${version}.tar.xz dcc4b54cc5565670a65581760261c163d720991f0d06486da61f8d839b52de27"
auth_type="sha256"
useconfigure=true
use_fresh_config_sub=true

pre_configure() {
    run "./Util/preconfig"
}
