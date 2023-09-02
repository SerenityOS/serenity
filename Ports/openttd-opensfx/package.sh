#!/usr/bin/env -S bash ../.port_include.sh
port=openttd-opensfx
version=1.0.3
workdir=.
files=(
    "https://cdn.openttd.org/opensfx-releases/${version}/opensfx-${version}-all.zip#e0a218b7dd9438e701503b0f84c25a97c1c11b7c2f025323fb19d6db16ef3759"
)

build() {
    # The ZIP file we downloaded contains a tarball.
    run_nocd tar xf opensfx-${version}.tar
}

install() {
    run_nocd mkdir -p ${SERENITY_INSTALL_ROOT}/usr/local/share/games/openttd/baseset/
    run_nocd cp -a opensfx-${version}/* ${SERENITY_INSTALL_ROOT}/usr/local/share/games/openttd/baseset/
}
