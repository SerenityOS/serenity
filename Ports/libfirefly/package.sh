#!/usr/bin/env -S bash ../.port_include.sh
project_name=firefly
# shellcheck disable=SC2034
useconfigure=true
# shellcheck disable=SC2034
port="lib${project_name}"
version=2.0.0
# shellcheck disable=SC2034
files=(
    "https://github.com/tbhaxor/${project_name}/archive/refs/tags/v${version}.tar.gz firefly-${version}.tar.gz 9df3b69bac287948f1f52d337c63e1cfb8941e78cb9bd3e2fbce6aeaea34f481"
)
# shellcheck disable=SC2034
workdir="${project_name}-${version}"
# shellcheck disable=SC2034
makeopts=(-Cbuild)
# shellcheck disable=SC2034
installopts=(-Cbuild)

configure() {
    run cmake -Bbuild
}
