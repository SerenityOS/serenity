#!/usr/bin/env -S bash ../.port_include.sh
port="libyaml"
version="0.2.5"
files="https://github.com/yaml/libyaml/archive/refs/tags/${version}.tar.gz yaml-${version}.tar.gz fa240dbf262be053f3898006d502d514936c818e422afdcf33921c63bed9bf2e"
useconfigure="true"
auth_type="sha256"

pre_configure() {
    run ./bootstrap
}
