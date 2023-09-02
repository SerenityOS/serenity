#!/usr/bin/env -S bash ../.port_include.sh
port=openttd-opengfx
version=7.1
workdir=.
files=(
    "https://cdn.openttd.org/opengfx-releases/${version}/opengfx-${version}-all.zip#928fcf34efd0719a3560cbab6821d71ce686b6315e8825360fba87a7a94d7846"
)

build() {
    # The ZIP file we downloaded contains a tarball.
    run_nocd tar xf opengfx-${version}.tar
}

install() {
    run_nocd mkdir -p ${SERENITY_INSTALL_ROOT}/usr/local/share/games/openttd/baseset/
    run_nocd cp -a opengfx-${version}/* ${SERENITY_INSTALL_ROOT}/usr/local/share/games/openttd/baseset/
}
