#!/usr/bin/env -S bash ../.port_include.sh
port='openttd-opengfx'
version='8.0'
workdir='.'
files=(
    "https://cdn.openttd.org/opengfx-releases/${version}/opengfx-${version}-all.zip#43a0c1dabf39cb865394f3a6cc36d4da5c10ecfaaf55652043104806810903be"
)

build() {
    # The ZIP file we downloaded contains a tarball.
    run_nocd tar xf opengfx-${version}.tar
}

install() {
    run_nocd mkdir -p ${SERENITY_INSTALL_ROOT}/usr/local/share/games/openttd/baseset/
    run_nocd cp -a opengfx-${version}/* ${SERENITY_INSTALL_ROOT}/usr/local/share/games/openttd/baseset/
}
